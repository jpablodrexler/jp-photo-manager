---
name: dependency-upgrade
description: >
  Routine dependency-upgrade workflow. TRIGGER when asked to upgrade
  dependencies, bump package versions, check for outdated packages, or do
  routine dependency maintenance. This is distinct from `security-reviewer`
  §1, which reactively flags a dependency with a known CVE while reviewing
  code — this skill proactively drives the update-and-regression-test cycle
  itself, on a cadence, whether or not anything is currently flagged as
  vulnerable. The package managers, survey commands, special packages and
  per-group test commands come from the `Dependency upgrade
  checks` section of the project's docs.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Dependency Upgrade Skill

Bump outdated dependencies in every manifest the project has, verify nothing
broke, and leave a clear record of what changed, what was skipped, and why —
without silently walking into a major-version breaking change.

`<reports>` below is `docs/reports` unless `.backlog-kit.toml` sets
`[paths] backlog_dir`, in which case it is the `reports` folder inside that
directory's parent folder. The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Never run `git commit`, `git push` or open a PR.**

## Workflow

1. **Scope.** Which part of the project (each package manifest or module the
   project has) — ask if not specified.
2. **Branch.** If not already on a dedicated branch, use the `gitflow` skill
   ("start feature") to cut one — name it something like
   `dependency-upgrade-{YYYY-MM-DD}` unless the user gives a different name.
3. **Project checks.** Read the `Dependency upgrade checks` section of the
   project's `docs/frontend.md` and/or `docs/backend.md` (the kit convention:
   each of those docs holds the conventions of its side). If the docs have no
   such section, work from the frame above plus what the project's CLAUDE.md
   and docs say, and tell the user that no project checks were found.
4. **Survey.** Run the discovery commands in §1/§2 below and build a list of
   outdated dependencies, each classified **patch**, **minor**, or **major**
   (semver terms — a major bump is any change to the leftmost non-zero
   version component).
5. **Decide scope of the bump** (§3): patch and minor bumps apply by
   default; major bumps are listed but not applied without the user opting
   in per-package, since they can be breaking.
6. **Apply in groups, testing after each group** (§4) — not all dependencies
   in one shot. If a group's test run fails, bisect within that group rather
   than reverting everything.
7. **Cross-check against `security-reviewer` §1** (§5) — confirm the bump
   actually resolved any CVE it was meant to address, and re-run the
   vulnerability scan to catch anything the survey step missed.
8. **Report** (§6): what was bumped, what was left for the user to decide
   (majors), and what broke and how it was resolved.
9. **Never commit.** Leave everything staged/unstaged in the working tree
   for the user to review — same convention as the review skills' Fix
   Workflows.

---

## 1. First Survey

Project-specific: the project's checks give the discovery commands for this
part of the project (outdated listing, vulnerability scan, unused-dependency
report) and any flags to raise on a stale tool or dependency.

A dependency the outdated listing shows but the unused-dependency report
also flags as genuinely unused (and that is not one of the documented
exceptions resolved by name rather than import) is a candidate for removal
instead of upgrade — no point spending a bump on a package nothing
references anymore. A dated snapshot of that report, if the project
produces one, lives under `<reports>/dead-code/`.

---

## 2. Second Survey and Special Packages

Project-specific: the project's checks cover any further manifest to survey and
the packages that must not be bumped with a plain install (framework
updaters that codemod breaking changes and keep a package family on matching
versions, client libraries every service is built directly on top of, test
tooling with its own upgrade path).

---

## 3. Classify & Decide

| Bump type | Default action |
|---|---|
| Patch (`x.y.Z`) | Apply automatically |
| Minor (`x.Y.z`) | Apply automatically |
| Major (`X.y.z`) | List for the user; apply only the ones they explicitly approve |

🔴 Never apply a major-version bump to a framework-defining dependency, or
to any package the project's checks name, without explicit user approval for
that specific package — these routinely carry breaking changes the
project's tests may not fully cover.

🟢 A patch/minor bump to a leaf dependency with no direct API usage in this
codebase is lower risk than a direct, heavily-used one — note this
distinction in the report (§6) but the automatic/manual split above still
applies uniformly by bump type, not by usage weight.

---

## 4. Apply & Test, In Groups

Don't bump every dependency and run the suite once at the end — if it
fails, you won't know which bump caused it. Group by blast radius instead.

Project-specific: the project's checks list the groups in order, the command
that verifies each one, and what to read before applying a major bump of
each framework-defining dependency. Each approved major bump is its own
group, applied one at a time with its own test run immediately after.

If a group's test run fails, narrow it down within that group (revert half,
retest) rather than reverting the whole session's work.

---

## 5. Cross-Check Against Security Findings

After applying bumps, re-run the vulnerability scan from
`security-reviewer` §1 (the project's checks give the commands).

If a CVE that motivated this upgrade session is still present, the fix
either isn't in the version reached yet (may need a further major bump the
user hasn't approved) or the vulnerable dependency is transitive and needs
an explicit override entry — don't report the upgrade as complete until this
is confirmed one way or the other.

---

## 6. Report

Summarize:

- **Applied** — each bumped dependency, old → new version, grouped by §4's
  groups, and which manifest it lives in.
- **Skipped (major, needs approval)** — every major-version bump found in
  the survey that wasn't applied.
- **Broke & fixed** — anything a test run caught, and what was changed to
  resolve it (a test expectation update, a config change, or a revert of
  that one specific bump if it couldn't be resolved in-session).
- **Security** — whether §5's re-scan confirms the motivating CVE (if any)
  is resolved.

Do not run `git add`/`git commit` as part of this report — the summary is
the deliverable; the diff stays in the working tree for the user.
