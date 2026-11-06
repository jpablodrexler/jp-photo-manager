#!/usr/bin/env python3
# VENDORED from lib/gitread.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
The only place in backlog-kit that talks to git, and it is deliberately
read-mostly: an allowlist of subcommands, none of which can create a commit,
move a ref to new content, or publish anything.

    status rev-parse branch rev-list symbolic-ref for-each-ref diff ls-files log   (read)
    fetch                                                      (updates remote-tracking refs only)

`branch` is further limited to listing/showing (an allowlist of flags, so `git branch <name>`
cannot create a ref), and flags that write a file (`diff --output`) are refused. The kit's public promise — nothing is committed, pushed or opened as a PR unless
the user asks for it — is enforced here in code for every script (none of them can
write), and by tests/test_skills.py, which fails if any other script imports subprocess.
The `gitflow` skill performs its (confirmed) writes itself, never through this module.
Branch creation/switching is done by the work skills' subagents and is
documented there; it is not routed through this module.
"""
import subprocess

READ_ONLY = {"status", "rev-parse", "branch", "rev-list", "symbolic-ref",
             "for-each-ref", "diff", "ls-files", "log"}
ALLOWED = READ_ONLY | {"fetch"}
# `branch` is limited to listing: only these flags, and a bare name is accepted only as the value
# of a flag that takes one (`--merged <ref>`); anything else (`git branch sneaky`) would create a ref.
BRANCH_FLAGS = {"--show-current", "--list", "-l", "-r", "--remotes", "-a", "--all", "-v", "-vv",
                "--merged", "--no-merged", "--contains", "--no-contains", "--points-at", "--no-color"}
BRANCH_VALUE_FLAGS = {"--merged", "--no-merged", "--contains", "--no-contains", "--points-at"}
BRANCH_PREFIXES = ("--format=", "--sort=", "--merged=", "--no-merged=", "--contains=", "--no-contains=",
                   "--points-at=", "--abbrev=")
FETCH_REFUSED = {"--update-head-ok", "--force", "-f", "--upload-pack", "--receive-pack"}
# Flags that make an otherwise read-only command write a file or run a configured program.
UNSAFE_PREFIXES = ("--output", "--ext-diff", "--textconv", "--upload-pack", "--exec")


class GitError(RuntimeError):
    pass


def _check_branch(args):
    expect_value = False
    for a in args:
        if expect_value:
            expect_value = False
            if not a.startswith("-"):
                continue
        if a in BRANCH_FLAGS:
            expect_value = a in BRANCH_VALUE_FLAGS
        elif not a.startswith(BRANCH_PREFIXES):
            raise GitError("git branch is limited to listing / showing the current branch")


def _run(root, *args):
    if not args or args[0] not in ALLOWED:
        raise GitError(f"git {args[0] if args else ''} is not allowed from backlog-kit scripts")
    if args[0] == "branch":
        _check_branch(args[1:])
    if args[0] == "fetch" and (FETCH_REFUSED & set(args[1:]) or any(a.startswith("--upload-pack") for a in args)):
        raise GitError("git fetch flag not allowed")
    if any(a.startswith(UNSAFE_PREFIXES) for a in args[1:]):
        raise GitError("this git flag can write a file or run a program and is not allowed")
    # quotepath=false: report non-ASCII paths as they are instead of as "\303\251"-style quoted strings.
    return subprocess.run(["git", "-C", str(root), "-c", "core.quotepath=false", *args],
                          capture_output=True, text=True, encoding="utf-8", errors="replace")


def git(root, *args, check=True):
    """Run an allowlisted git subcommand in `root`; return stdout (text)."""
    proc = _run(root, *args)
    if check and proc.returncode != 0:
        raise GitError(proc.stderr.strip() or f"git {args[0]} failed")
    return proc.stdout


def ok(root, *args):
    """True if the allowlisted git command exits 0 (never raises on a non-zero exit)."""
    return _run(root, *args).returncode == 0


def changed_files(root):
    """Every path with working-tree or index changes, untracked files included.

    Uses `status --porcelain=v1 -z -uall` — the reliable way to see new files too
    (`git diff --name-only HEAD` misses untracked ones, and a branch-to-branch diff
    shows nothing when nothing is committed). Renames report the new path.
    """
    out = git(root, "status", "--porcelain=v1", "-z", "-uall")
    files, parts, i = [], out.split("\0"), 0
    while i < len(parts):
        entry = parts[i]
        i += 1
        if len(entry) < 4:
            continue
        status, path = entry[:2], entry[3:]
        if "R" in status or "C" in status:
            i += 1  # skip the "from" path that follows a rename/copy entry
        files.append(path)
    return sorted(set(files))


def staged_files(root):
    """Paths already in the index (what a bare `git commit` would take)."""
    out = git(root, "diff", "--cached", "--name-only", "-z", check=False)
    return sorted({p for p in out.split("\0") if p})
