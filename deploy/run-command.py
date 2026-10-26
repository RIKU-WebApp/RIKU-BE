"""Publish one CI artifact and wait for the instance's final deployment result."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time


def aws(*args):
    result = subprocess.run(["aws", *args, "--output", "json", "--no-cli-pager"],
                            check=True, text=True, capture_output=True, timeout=90)
    return json.loads(result.stdout or "null")


def main():
    env = os.environ
    artifact = Path("artifact/app.jar")
    with artifact.open("rb") as stream:
        checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    key = (f'releases/{env["DEPLOY_ENV"]}/{env["GITHUB_SHA"]}/'
           f'{env["GITHUB_RUN_ID"]}-{env["GITHUB_RUN_ATTEMPT"]}/app.jar')
    # Conditional upload makes a run/attempt immutable, including accidental retries.
    aws("s3api", "put-object", "--bucket", env["ARTIFACT_BUCKET"], "--key", key,
        "--body", str(artifact), "--if-none-match", "*", "--server-side-encryption", "AES256")
    command = aws("ssm", "send-command", "--instance-ids", env["INSTANCE_ID"],
                  "--document-name", env["DOCUMENT_NAME"], "--document-version", "1",
                  "--parameters", json.dumps({"ArtifactKey": [key], "Sha256": [checksum]}),
                  "--timeout-seconds", "120", "--comment", f'RIKU {env["GITHUB_SHA"]}')
    command_id = command["Command"]["CommandId"]
    print(f"SSM command: {command_id}", flush=True)
    deadline = time.monotonic() + 1100
    while time.monotonic() < deadline:
        try:
            result = aws("ssm", "get-command-invocation", "--command-id", command_id,
                         "--instance-id", env["INSTANCE_ID"])
        except subprocess.CalledProcessError as error:
            if "InvocationDoesNotExist" not in error.stderr:
                raise
            time.sleep(5)
            continue
        status = result["Status"]
        if status == "Success":
            print(result.get("StandardOutputContent", ""))
            return
        if status not in ("Pending", "InProgress", "Delayed"):
            print(result.get("StandardErrorContent", ""))
            raise RuntimeError(f"Deployment failed: {status}; inspect SSM command {command_id}")
        time.sleep(5)
    # Do not cancel a potentially rolling-back command just because the runner timed out.
    raise TimeoutError(f"Deployment not confirmed; inspect SSM command {command_id}")


if __name__ == "__main__":
    main()
