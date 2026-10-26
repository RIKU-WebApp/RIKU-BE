#!/usr/bin/env bash
# Installed root-owned at /usr/local/sbin/riku-deploy; invoked only by fixed SSM documents.
set -Eeuo pipefail
umask 027
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
[[ $EUID -eq 0 && $# -eq 3 ]] || { echo 'Expected root: riku-deploy prod|dev artifact-key sha256' >&2; exit 2; }
environment=$1 artifact_key=$2 checksum=$3
[[ $environment == prod || $environment == dev ]] || exit 2
[[ $artifact_key =~ ^releases/$environment/([0-9a-f]{40})/([0-9]+-[0-9]+)/app\.jar$ ]] || exit 2
release_id="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
[[ $checksum =~ ^[0-9a-f]{64}$ ]] || exit 2
# This configuration and the script must never be writable by the application user.
source /etc/RIKU/deploy.conf
: "${ARTIFACT_BUCKET:?}" "${AWS_REGION:?}"
export AWS_REGION
app_dir="/home/ubuntu/apps/$environment"
release_root="/opt/riku/releases/$environment"
service="riku-$environment.service"
port=9090 app_port=8080
[[ $environment == prod ]] || { port=9091; app_port=8081; }
health_url="http://127.0.0.1:$port/actuator/health/readiness"
legacy_url="http://127.0.0.1:$app_port/v3/api-docs"
exec 9>"/run/lock/riku-$environment-deploy.lock"
flock -n 9 || { echo 'Another deployment is running' >&2; exit 3; }
[[ -f "$app_dir/app.jar" ]] || { echo 'Bootstrap an existing release before automated deployment' >&2; exit 4; }
systemctl is-active --quiet "$service"
previous_probe=$health_url
curl --fail --silent --max-time 5 "$health_url" >/dev/null || {
    curl --fail --silent --max-time 5 "$legacy_url" >/dev/null
    previous_probe=$legacy_url
}
install -d -m 750 -o root -g ubuntu /opt/riku /opt/riku/releases "$release_root"
release_dir="$release_root/$release_id"
[[ ! -e "$release_dir" ]] || { echo 'Release already exists; rerun the workflow with a new attempt' >&2; exit 5; }
mkdir "$release_dir"
chown root:ubuntu "$release_dir"
chmod 750 "$release_dir"
timeout 120 aws s3 cp "s3://$ARTIFACT_BUCKET/$artifact_key" "$release_dir/app.jar" \
    --only-show-errors --cli-connect-timeout 10 --cli-read-timeout 30
printf '%s  %s\n' "$checksum" "$release_dir/app.jar" | sha256sum --check --status
chown root:ubuntu "$release_dir/app.jar"
chmod 640 "$release_dir/app.jar"

previous=$(readlink -f "$app_dir/app.jar")
if [[ ! -L "$app_dir/app.jar" ]]; then
    # Preserve the legacy fixed-name JAR before the very first deployment.
    previous="$release_root/legacy-$(date +%s%N).jar"
    cp --preserve=mode,ownership,timestamps "$app_dir/app.jar" "$previous"
fi
switched=false
wait_ready() {
    local url=$1
    for ((attempt=0; attempt<30; attempt++)); do
        if systemctl is-active --quiet "$service" && curl --fail --silent --max-time 3 "$url" >/dev/null; then
            return 0
        fi
        sleep 2
    done
    return 1
}
switch_to() {
    ln -sfn "$1" "$app_dir/.app.jar.next.$$"
    mv -Tf "$app_dir/.app.jar.next.$$" "$app_dir/app.jar"
}
rollback() {
    local code=$?
    trap - EXIT INT TERM
    if (( code != 0 )) && [[ $switched == true ]]; then
        echo 'Deployment failed; restoring previous JAR' >&2
        if switch_to "$previous" && systemctl restart "$service" && wait_ready "$previous_probe"; then
            echo 'Previous release restored' >&2
        else
            echo 'ROLLBACK FAILED: operator intervention required' >&2
        fi
    fi
    exit "$code"
}
trap rollback EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
switched=true
switch_to "$release_dir/app.jar"
systemctl restart "$service"
wait_ready "$health_url"
switched=false
trap - EXIT INT TERM
echo "Deployed $environment $release_id (readiness includes database connectivity)"

# Keep five recent releases plus the last working release. Never recursively delete.
prune_releases() {
    local candidate target index
    local -a releases
    mapfile -t releases < <(find "$release_root" -mindepth 1 -maxdepth 1 -type d \
        -printf '%T@ %f\n' | sort -nr | cut -d' ' -f2-)
    for ((index=5; index<${#releases[@]}; index++)); do
        candidate=${releases[index]}
        [[ $candidate =~ ^[0-9a-f]{40}-[0-9]+-[0-9]+$ ]] || continue
        target="$release_root/$candidate"
        [[ $target != "$release_dir" && "$target/app.jar" != "$previous" ]] || continue
        rm -f -- "$target/app.jar" && rmdir -- "$target" || return 1
    done
}
prune_releases || echo 'Release cleanup failed; deployment is healthy, inspect disk usage' >&2
