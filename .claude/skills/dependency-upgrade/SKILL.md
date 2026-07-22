---
name: dependency-upgrade
description: >
  Routine dependency-upgrade workflow for the JPPhotoManager web application
  (Maven backend + npm frontend). TRIGGER when asked to upgrade dependencies,
  bump package versions, check for outdated packages, or do routine
  dependency maintenance. This is distinct from `security-reviewer` §1,
  which reactively flags dependencies with a known CVE while reviewing
  code — this skill proactively drives the update-and-regression-test cycle
  itself, on a cadence, whether or not anything is currently flagged as
  vulnerable.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Dependency Upgrade Skill

Bump outdated Maven and npm dependencies, verify nothing broke, and leave a
clear record of what changed, what was skipped, and why — without silently
walking into a major-version breaking change.

## Workflow

1. **Scope.** Backend only, frontend only, or both — ask if not specified.
2. **Branch.** If not already on a dedicated branch, use the `gitflow` skill
   ("start feature") to cut one — name it something like
   `dependency-upgrade-{YYYY-MM-DD}` unless the user gives a different name.
   Don't run this workflow directly on `develop`.
3. **Survey.** Run the discovery commands in §1/§2 below and build a list of
   outdated dependencies, each classified **patch**, **minor**, or **major**
   (semver terms — a major bump is any change to the leftmost non-zero
   version component).
4. **Decide scope of the bump** (§3): patch and minor bumps apply by
   default; major bumps are listed but not applied without the user opting
   in per-package, since they can be breaking.
5. **Apply in groups, testing after each group** (§4) — not all dependencies
   in one shot. If a group's test run fails, bisect within that group rather
   than reverting everything.
6. **Cross-check against `security-reviewer` §1** (§5) — confirm the bump
   actually resolved any CVE it was meant to address, and re-run the
   vulnerability scan to catch anything the survey step missed.
7. **Report** (§6): what was bumped, what was left for the user to decide
   (majors), and what broke and how it was resolved.
8. **Never commit.** Leave everything staged/unstaged in the working tree
   for the user to review — same convention as the review skills' Fix
   Workflows. This applies regardless of the branch created in step 2; the
   branch is where the work happens, not authorization to commit it.

---

## 1. Backend Survey (Maven)

```bash
cd JPPhotoManagerWeb/backend
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

Most library versions in this project are inherited from the
`spring-boot-starter-parent` BOM (see `java-developer` §1) rather than
pinned individually — check the parent POM version itself
(`spring-boot-starter-parent`) as the primary lever; a Spring Boot patch/minor
bump usually moves every managed dependency's version together in a tested,
compatible set. Only bump an individual `<dependency>` version directly when
it's declared outside the BOM (not managed by `spring-boot-starter-parent`).

🟡 Flag any dependency currently overridden to an older version via
`<dependencyManagement>` that the survey shows is now behind — verify
whether the override reason (check the POM for a comment) still applies
before removing it.

---

## 2. Frontend Survey (npm)

```bash
cd JPPhotoManagerWeb/frontend
npm outdated
npm audit --audit-level=high
```

**Angular packages are a special case.** Don't bump `@angular/*` packages
with a raw `npm install <pkg>@latest` — Angular major versions ship
migration schematics that codemod breaking changes (standalone-components
migration, control-flow syntax, etc.). Use the Angular CLI's own updater
instead, which also validates that the whole `@angular/*` family stays on
matching versions:

```bash
npx ng update                     # lists available updates, including peer-dependency-safe ordering
npx ng update @angular/core @angular/cli   # apply one major step at a time
```

Angular's own guidance is to upgrade one major version at a time (not skip
versions) — if `npm outdated` shows Angular is more than one major behind,
plan multiple sequential `ng update` passes, each with its own test run
(§4), rather than jumping straight to latest.

---

## 3. Classify & Decide

| Bump type | Default action |
|---|---|
| Patch (`x.y.Z`) | Apply automatically |
| Minor (`x.Y.z`) | Apply automatically |
| Major (`X.y.z`) | List for the user; apply only the ones they explicitly approve |

🔴 Never apply a major-version bump to `spring-boot-starter-parent`,
`@angular/core`, or any other framework-defining dependency without explicit
user approval for that specific package — these routinely carry breaking
changes this project's tests may not fully cover (e.g. a removed
auto-configuration, a changed default).

🟢 A patch/minor bump to a leaf dependency with no direct API usage in this
codebase (a transitive-only dependency bumped for a CVE fix) is lower risk
than a direct, heavily-used dependency (Spring Data JPA, MapStruct, Angular
Material) — note this distinction in the report (§6) but the automatic/
manual split above still applies uniformly by bump type, not by usage
weight.

---

## 4. Apply & Test, In Groups

Don't bump every dependency and run the suite once at the end — if it fails,
you won't know which bump caused it. Group by blast radius instead:

1. **Group 1 — backend patch/minor**, applied via the parent POM bump plus
   any individually-managed dependencies from §1. Then:
   ```bash
   cd JPPhotoManagerWeb/backend && mvn clean test
   ```
2. **Group 2 — frontend patch/minor** (`npm update`, or `ng update` for any
   `@angular/*` minor). Then:
   ```bash
   cd JPPhotoManagerWeb/frontend && npm test && npm run lint && npm run build:prod
   ```
3. **Group 3+ — each approved major bump, one at a time**, its own test run
   immediately after. For a Spring Boot major/minor jump, also skim the
   official release notes' "breaking changes"/migration guide section before
   applying — don't rely on the test suite alone to catch a removed
   auto-configuration the tests don't happen to exercise.

If a group's test run fails, narrow it down within that group (revert half,
retest) rather than reverting the whole session's work — the goal is to land
as much of the safe upgrade as possible, not to give up on the first
failure.

---

## 5. Cross-Check Against Security Findings

After applying bumps, re-run the vulnerability scan from
`security-reviewer` §1:

```bash
cd JPPhotoManagerWeb/backend && mvn org.owasp:dependency-check-maven:check
cd JPPhotoManagerWeb/frontend && npm audit --audit-level=high
```

If a CVE that motivated this upgrade session is still present, the fix
either isn't in the version reached yet (may need a further major bump the
user hasn't approved) or the vulnerable dependency is transitive and needs
an explicit `<dependencyManagement>`/npm `overrides` entry — don't report
the upgrade as complete until this is confirmed one way or the other.

---

## 6. Report

Summarize:

- **Applied** — each bumped dependency, old → new version, grouped by §4's
  groups.
- **Skipped (major, needs approval)** — every major-version bump found in
  the survey that wasn't applied, so the user has the full picture even for
  what they didn't ask for yet.
- **Broke & fixed** — anything a test run caught, and what was changed to
  resolve it (a test expectation update, a config change, or a revert of
  that one specific bump if it couldn't be resolved in-session).
- **Security** — whether §5's re-scan confirms the motivating CVE (if any)
  is resolved.

Do not run `git add`/`git commit` as part of this report — the summary is
the deliverable; the diff stays in the working tree for the user.
