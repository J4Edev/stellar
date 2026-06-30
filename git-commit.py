#!/usr/bin/env python3

import subprocess
import shutil
import sys

def run(cmd, capture=False):
    if capture:
        return subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, text=True)
    return subprocess.run(cmd, shell=True)

def check_program(name):
    return shutil.which(name) is not None

def safe_input(prompt, default=None):
    try:
        v = input(prompt).strip()
    except EOFError:
        v = ""
    if v == "" and default is not None:
        return default
    return v

def main():
    if not check_program("git"):
        print("Error: git is not installed or not in PATH.")
        sys.exit(1)

    print("\nGit Commit Helper\n------------------\n")

    do_add = safe_input("Stage all changes (git add .)? [Y/n] ", "y").lower() not in ("n", "no")
    if do_add:
        print("-> git add .")
        run("git add .")

    status = run("git status --porcelain", capture=True)
    dirty = status.stdout.strip() != ""

    if not dirty:
        print("Working tree clean — nothing to commit.")
        force_empty = safe_input("Create an empty commit anyway? [y/N] ", "n").lower() in ("y", "yes")
        if not force_empty:
            print("Aborting — no commit created.")
            return

    msg = safe_input("Commit message: ", None)
    while not msg:
        print("Commit message cannot be empty.")
        msg = safe_input("Commit message: ", None)

    print(f'-> git commit -m "{msg}"')
    res = run(f'git commit -m "{msg}"')
    if res.returncode != 0:
        print("Commit may have failed. See message above.")
        return

    do_push = safe_input("Push to current remote and branch? [y/N] ", "n").lower() in ("y", "yes")
    if do_push:
        print("-> git push")
        push = run("git push")
        if push.returncode != 0:
            print("\nPush failed. Common reasons:")
            print("- No login / PAT / SSH not set up")
            print("- Remote doesn't exist")
            print("- Branch not tracked")
            print("Paste the error above if you want help fixing it.")
    else:
        print("Skipping push.")

    print("\nDone!")

if __name__ == "__main__":
    main()
