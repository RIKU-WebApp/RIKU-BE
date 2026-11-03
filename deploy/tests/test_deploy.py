"""Exercise the real shell control flow with isolated paths and mocked OS/AWS commands."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "deploy.sh"
BASH = "C:/Program Files/Git/bin/bash.exe" if os.name == "nt" else shutil.which("bash")
COMMIT = "a" * 40
KEY = f"releases/prod/{COMMIT}/1-1/app.jar"
CHECKSUM = hashlib.sha256(b"new").hexdigest()
STUB = r'''#!/usr/bin/env bash
set -eu
case "${0##*/}" in
  aws) [[ $MODE != download_fail ]] || exit 1; printf '%s' new > "$4" ;;
  systemctl)
    if [[ $1 == restart ]]; then
      printf 'restart\n' >> "$TEST_ROOT/restarts"
      [[ $MODE != restart_fail || $(cat "$TEST_ROOT/apps/prod/app.jar") != new ]] || exit 1
    fi ;;
  curl)
    current=$(cat "$TEST_ROOT/apps/prod/app.jar")
    [[ $MODE != readiness_fail || $current != new ]] || exit 22
    [[ $MODE != rollback_fail || ! -e $TEST_ROOT/restarts ]] || exit 22
    [[ $MODE != legacy || "$*" != *9090* || $current != old ]] || exit 22 ;;
  flock) [[ $MODE != locked ]] ;;
  sleep|chown|chmod) : ;;
esac
'''


class DeployTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for directory in ("apps/prod", "etc", "locks", "bin"):
            (self.root / directory).mkdir(parents=True)
        self.jar = self.root / "apps/prod/app.jar"
        self.jar.write_bytes(b"old")
        (self.root / "etc/deploy.conf").write_text("ARTIFACT_BUCKET=test-bucket\nAWS_REGION=ap-northeast-2\n")
        for name in ("aws", "systemctl", "curl", "flock", "sleep", "chown", "chmod"):
            path = self.root / "bin" / name
            path.write_text(STUB, newline="\n")
            path.chmod(0o755)
        source = SCRIPT.read_text()
        self.assertIn("[[ $EUID -eq 0 && $# -eq 3 ]]", source)
        source = source.replace("[[ $EUID -eq 0 && $# -eq 3 ]]", "[[ $# -eq 3 ]]")
        shell_root = self.root.as_posix()
        if os.name == "nt":
            shell_root = "/" + shell_root[0].lower() + shell_root[2:]
        source = source.replace("export PATH=", f'export PATH="{shell_root}/bin":')
        source = source.replace("install -d -m 750 -o root -g ubuntu", "mkdir -p")
        for original, replacement in (("/home/ubuntu/apps", "apps"), ("/opt/riku", "riku"),
                                      ("/etc/RIKU", "etc"), ("/run/lock", "locks")):
            source = source.replace(original, (self.root / replacement).as_posix())
        self.script = self.root / "deploy.sh"
        self.script.write_text(source, newline="\n")

    def run_deploy(self, mode="success", key=KEY, checksum=CHECKSUM):
        return subprocess.run([BASH, self.script.as_posix(), "prod", key, checksum],
                              env={**os.environ, "MODE": mode, "TEST_ROOT": self.root.as_posix()},
                              text=True, capture_output=True, timeout=30)

    def test_success_keeps_previous_jar(self):
        result = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.jar.read_bytes(), b"new")
        self.assertEqual(next((self.root / "riku/releases/prod").glob("legacy-*.jar")).read_bytes(), b"old")

    def test_legacy_readiness_fallback(self):
        result = self.run_deploy("legacy")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.jar.read_bytes(), b"new")

    def test_bad_checksum_does_not_restart_or_replace_old_jar(self):
        result = self.run_deploy(checksum="0" * 64)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.jar.read_bytes(), b"old")
        self.assertFalse((self.root / "restarts").exists())

    def test_failed_download_does_not_restart(self):
        result = self.run_deploy("download_fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.jar.read_bytes(), b"old")
        self.assertFalse((self.root / "restarts").exists())

    def test_failed_readiness_restores_previous_jar_and_reports_failure(self):
        result = self.run_deploy("readiness_fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.jar.read_bytes(), b"old")
        self.assertIn("Previous release restored", result.stderr)
        self.assertEqual((self.root / "restarts").read_text().count("restart"), 2)

    def test_restart_failure_also_rolls_back(self):
        result = self.run_deploy("restart_fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.jar.read_bytes(), b"old")
        self.assertIn("Previous release restored", result.stderr)

    def test_failed_rollback_is_not_reported_as_success(self):
        result = self.run_deploy("rollback_fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ROLLBACK FAILED", result.stderr)

    def test_lock_blocks_competing_deployments(self):
        result = self.run_deploy("locked")
        self.assertEqual(result.returncode, 3)
        self.assertFalse((self.root / "restarts").exists())

    def test_path_traversal_or_wrong_environment_never_reaches_aws(self):
        for key in ("../app.jar", KEY.replace("prod", "dev"), KEY + "; touch bad"):
            self.assertEqual(self.run_deploy(key=key).returncode, 2)
        self.assertFalse((self.root / "riku").exists())

    def test_reusing_release_id_is_rejected(self):
        self.assertEqual(self.run_deploy().returncode, 0)
        result = self.run_deploy()
        self.assertEqual(result.returncode, 5)
        self.assertEqual((self.root / "restarts").read_text().count("restart"), 1)


if __name__ == "__main__":
    unittest.main()
