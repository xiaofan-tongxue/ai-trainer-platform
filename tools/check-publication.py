"""Reject common secrets and instance data before publishing; never print matches."""
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
GIT = ["git", "-c", "safe.directory=" + ROOT.as_posix(), "-C", str(ROOT)]
RULES = {
    "provider token": rb"\bsk-[A-Za-z0-9_-]{20,}",
    "GitHub token": rb"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})",
    "private key": rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
    "personal Windows profile": rb"(?i)C:[\\/]+Users[\\/]+(?!Public\b|Default\b|<)[A-Za-z0-9_.-]+",
}
BLOCKED = {"runtime", "backups", "build", "dist"}
SUFFIXES = {".dpapi", ".enc", ".pem", ".key", ".pfx", ".p12", ".log", ".bak"}


def main():
    findings = []
    paths = subprocess.check_output(GIT + ["ls-files", "-z"]).decode().split("\0")
    for name in filter(None, paths):
        path = pathlib.PurePosixPath(name)
        if (path.parts[0] in BLOCKED or path.suffix.lower() in SUFFIXES
                or (path.name.startswith(".env") and path.name != ".env.example")):
            findings.append({"file": name, "rule": "instance or secret file"})
        content = subprocess.check_output(GIT + ["show", ":" + name])
        for label, pattern in RULES.items():
            if re.search(pattern, content):
                findings.append({"file": name, "rule": label})
    for commit in subprocess.check_output(GIT + ["rev-list", "HEAD"]).decode().split():
        emails = subprocess.check_output(GIT + ["show", "-s", "--format=%ae%n%ce", commit]).decode().splitlines()
        if any(not email.endswith("@users.noreply.github.com") for email in emails):
            findings.append({"commit": commit, "rule": "use GitHub private commit email"})
    print(json.dumps({"findings": findings}, ensure_ascii=False))
    return bool(findings)


if __name__ == "__main__":
    sys.exit(main())
