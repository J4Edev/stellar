#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys

def run(cmd, capture=False):
    if capture:
        return subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    else:
        return subprocess.run(cmd, shell=True)

def check_program(name):
    return shutil.which(name) is not None

def safe_input(prompt, default=None):
    try:
        v = input(prompt)
    except EOFError:
        v = ""
    v = v.strip()
    if v == "" and default is not None:
        return default
    return v

def main():
    if not check_program("git"):
        print("Error: git is not installed or not in PATH. Install git first (sudo pacman -S git).")
        sys.exit(1)

    cwd = os.getcwd()
    repo_name = os.path.basename(os.path.abspath(cwd))
    print(f"\nWorking directory: {cwd}")
    print(f"Repo name (from folder): {repo_name}\n")

    do_init = safe_input("Initialize a git repository here? [Y/n] ", "y").lower() not in ("n", "no")
    if not do_init:
        print("Aborted by user.")
        return

    try:
        print("\n-> git init")
        run("git init")
    except Exception as e:
        print("git init failed:", e)
        return

    br = run("git rev-parse --abbrev-ref HEAD", capture=True)
    branch = None
    if br.returncode == 0:
        branch = br.stdout.strip()
    if not branch:
        branch = "master"
    print(f"Current branch: {branch}")

    want_main = safe_input("Rename current branch to 'main'? [Y/n] ", "y").lower() not in ("n", "no")
    if want_main:
        print("-> git branch -m main")
        run("git branch -m main")
        branch = "main"

    want_add = safe_input("Stage all files (git add .)? [Y/n] ", "y").lower() not in ("n", "no")
    if want_add:
        print("-> git add .")
        run("git add .")

    status = run("git status --porcelain", capture=True)
    if status.returncode != 0:
        print("Warning: git status failed. Continuing anyway.")
    else:
        if status.stdout.strip() == "":
            print("No changes detected (working tree clean). Nothing to commit.")
            commit_now = safe_input("Still create an empty commit? [y/N] ", "n").lower() in ("y", "yes")
        else:
            commit_now = True

    if commit_now:
        msg = safe_input("Commit message (default: 'Initial commit'): ", "Initial commit")
        print(f"-> git commit -m \"{msg}\"")
        res = run(f'git commit -m "{msg}"')
        if res.returncode != 0:
            print("git commit may have failed (see message above).")
    else:
        print("Skipping commit step.")

    add_remote = safe_input("Add a remote (e.g. https://github.com/user/repo.git or git@github.com:user/repo.git)? [y/N] ", "n").lower() in ("y", "yes")
    if add_remote:
        remote_url = safe_input("Remote URL: ")
        if remote_url:
            existing = run("git remote get-url origin", capture=True)
            if existing.returncode == 0 and existing.stdout.strip():
                print("An 'origin' remote already exists with URL:", existing.stdout.strip())
                replace = safe_input("Replace it? [y/N] ", "n").lower() in ("y", "yes")
                if replace:
                    run(f"git remote set-url origin {remote_url}")
                else:
                    name = safe_input("Add under a different remote name (default 'origin2'): ", "origin2")
                    run(f"git remote add {name} {remote_url}")
            else:
                run(f"git remote add origin {remote_url}")
        else:
            print("Empty URL given — skipping adding remote.")

    do_push = safe_input(f"Push branch '{branch}' to remote 'origin' now? [y/N] ", "n").lower() in ("y", "yes")
    if do_push:
        print(f"-> git push -u origin {branch}")
        push = run(f"git push -u origin {branch}")
        if push.returncode != 0:
            print("\nPush failed. Common reasons:")
            print("- No network / not authenticated (with HTTPS you must use a PAT, or set up SSH).")
            print("- Remote branch protected or different default branch.")
            print("Paste full error output above if you want help troubleshooting.")
    else:
        print("Skipping push.")

    print("\nDone. Summary:")
    print("  - Repo:", os.path.abspath(cwd))
    print("  - Branch:", branch)
    rem = run("git remote -v", capture=True)
    if rem.returncode == 0:
        print("  - Remotes:\n" + rem.stdout)
    else:
        print("  - Remotes: (unable to get remote list)")

if __name__ == "__main__":
    main()
