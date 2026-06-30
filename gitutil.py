#!/usr/bin/env python3
import argparse
import subprocess
import sys


def run(cmd, check=True):
    print("$", " ".join(cmd))
    return subprocess.run(cmd, check=check)


def run_capture(cmd):
    return subprocess.run(cmd, capture_output=True, text=True, check=True).stdout


def get_branch():
    branch = run_capture(["git", "branch", "--show-current"]).strip()
    if not branch:
        print("Error: not on a branch (detached HEAD?). Aborting.")
        sys.exit(1)
    return branch


def has_local_changes():
    status = run_capture(["git", "status", "--porcelain"])
    return bool(status.strip())


def remote_branch_exists(branch):
    result = subprocess.run(
        ["git", "rev-parse", "--verify", "--quiet", f"refs/remotes/origin/{branch}"],
        capture_output=True,
    )
    return result.returncode == 0


def override():
    branch = get_branch()
    print(f"Using branch: {branch}")

    run(["git", "fetch", "origin"])

    if not remote_branch_exists(branch):
        print(
            f"Error: origin/{branch} does not exist. "
            f"Push the branch first or check it out from origin."
        )
        sys.exit(1)

    stashed = has_local_changes()
    if stashed:
        print("Stashing current working state...")
        run(["git", "stash", "push", "-u", "-m", "override-temp-stash"])
    else:
        print("Working tree is clean, nothing to stash.")

    print("Syncing with origin...")
    run(["git", "reset", "--hard", f"origin/{branch}"])

    if stashed:
        print("Restoring working state...")
        pop = run(["git", "stash", "pop"], check=False)
        if pop.returncode != 0:
            print(
                "\nWARNING: 'git stash pop' failed, most likely due to conflicts "
                "with the freshly synced branch.\n"
                "Your changes are still safe in the stash (not lost).\n"
                "Resolve the conflicts manually, then run:\n"
                "  git add .\n"
                "  git stash drop   # once you've confirmed everything is correct\n"
                "Aborting the rest of the override (no commit/push will happen)."
            )
            sys.exit(1)

    print("Staging changes...")
    run(["git", "add", "."])

    if not has_local_changes() and run_capture(["git", "diff", "--cached", "--name-only"]).strip() == "":
        print("Nothing staged after sync — working tree matches origin. Nothing to commit.")
        return

    msg = input("Commit message: ").strip()
    if not msg:
        print(
            "No commit message provided. Changes remain staged but uncommitted. Aborting."
        )
        sys.exit(1)

    run(["git", "commit", "-m", msg])

    push = input("Push to remote? [y/N]: ").lower()
    if push == "y":
        result = run(["git", "push", "origin", branch], check=False)
        if result.returncode != 0:
            print(
                f"\nPush failed. This usually means origin/{branch} moved since we "
                f"synced (someone else pushed in the meantime).\n"
                f"Re-run with --override to sync again, or resolve manually with "
                f"'git pull --rebase origin {branch}'."
            )
            sys.exit(1)


def sync():
    branch = get_branch()
    run(["git", "fetch", "origin"])
    if not remote_branch_exists(branch):
        print(f"Error: origin/{branch} does not exist.")
        sys.exit(1)
    run(["git", "checkout", branch])
    run(["git", "reset", "--hard", f"origin/{branch}"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sync", action="store_true")
    parser.add_argument("--override", action="store_true")
    args = parser.parse_args()

    if args.sync:
        sync()
    elif args.override:
        override()
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
