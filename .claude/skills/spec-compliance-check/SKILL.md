---
name: spec-compliance-check
description: >
  Verifies that a fully-implemented OpenSpec change actually satisfies the
  acceptance criteria in its own `specs/**/spec.md` files — not just that
  `tasks.md` is checked off. TRIGGER when asked to check spec compliance,
  verify a change is ready to archive, or audit whether a change's
  implementation matches its spec. Also a natural step to run before
  invoking `openspec-archive-change`, though it does not invoke that skill
  itself and is not invoked by it. This is a standalone, read-only
  companion skill — it never edits anything under `openspec/`, and it is
  not part of the `openspec-*` skill family (propose/explore/apply/archive)
  and never modifies those skills or their workflow.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Spec Compliance Check Skill

`<reports>` is `docs/reports` unless `.backlog-kit.toml` sets `[paths] backlog_dir` (then it is the `reports` folder next to the backlog's parent). The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

`tasks.md` tracks whether the implementation *steps* happened. It does not
tell you whether the resulting *behavior* matches what the spec promised —
a task can be checked off because code was written, without anyone
re-reading the spec's `GIVEN`/`WHEN`/`THEN` scenarios against what actually
ships. This skill closes that gap: it walks every `### Requirement` /
`#### Scenario` pair in a change's spec files and determines whether each
one is actually verified — by an automated test that currently passes, by a
direct manual check against a running instance, or neither.

This skill **produces a report and a verdict. It does not fix code, does
not edit specs or tasks, and does not archive the change.** Those remain
separate, human-directed decisions — this skill's only job is to tell you
honestly whether a change is ready for that decision.

**Never run `git commit`, `git push` or open a PR.**

## Project checks

Read the `Spec compliance checks` section of the project's `docs/frontend.md` and/or `docs/backend.md` (the kit convention: each of those docs holds the conventions of its side). It says which kinds of test file exist, how to run a single test and how to verify a scenario by hand. If the docs have no such section, work from the frame above plus what the project's CLAUDE.md and docs say, and tell the user that no project checks were found.

## Workflow

1. **Identify the change** (§1).
2. **Confirm it's implementation-complete** (§2) — don't bother checking
   compliance on a change that isn't finished yet.
3. **Extract every Requirement/Scenario** from the change's spec files (§3).
4. **Match each Scenario to verification evidence** (§4) — an automated
   test (preferred) or a manual check (fallback).
5. **Verify the evidence actually holds** (§5) — a matched test must
   currently pass; a manual check must actually be performed, not assumed.
6. **Report** (§6) with a per-scenario verdict and an overall recommendation.

---

## 1. Identify the Change

- If the user names a change explicitly, use `openspec/changes/<name>/`.
- Otherwise, infer it from the current branch: `git branch --show-current`
  — if it matches `feature/<name>`, use `openspec/changes/<name>/` (the
  same convention `feature-development` and `gitflow` use).
- If neither resolves to an existing, non-archived directory under
  `openspec/changes/` (i.e. not already under `openspec/changes/archive/`),
  stop and ask which change to check. If the resolved change is already
  archived, say so — this skill's primary use is pre-archive; checking a
  historical one is still possible but unusual, confirm that's really what
  the user wants before proceeding.

## 2. Confirm Implementation Is Complete

Read `openspec/changes/<name>/tasks.md`. If any task is still `- [ ]`
(unchecked), stop and report which ones — don't run a compliance check
against a partially-implemented change; the result would be noise, not
signal. This mirrors `openspec-apply-change`'s own gate for what "done"
means, without needing to touch that skill.

## 3. Extract Requirements & Scenarios

Read every file under `openspec/changes/<name>/specs/**/spec.md` (a change
can touch more than one capability — e.g. a change that both adds a new
capability and modifies an existing one). Each file follows a
fixed structure:

```
### Requirement: <normative statement, usually with SHALL>

<prose elaborating the requirement>

#### Scenario: <name>

- **GIVEN** <preconditions>
- **WHEN** <action — usually a specific request or UI interaction>
- **THEN** <expected outcome, usually a specific, checkable assertion>
```

Build a flat list of every Scenario across every spec file for this change,
each carrying its parent Requirement's text for context. Treat each
scenario as one independently checkable unit, not the requirement as a
whole (a requirement can be partially satisfied — e.g. the happy path
works but a validation-error or access-denial scenario under the same
requirement doesn't).

## 4. Match Each Scenario to Verification Evidence

For each scenario, look for evidence **in this order** — stop at the first
one found:

### 4.1 An automated test written for this change

`tasks.md`'s test-writing tasks are usually worded closely enough to a
scenario to match directly (e.g. a task *"Add test: a signed-out request
is rejected"* maps 1:1 to a spec's *"Scenario: Unauthenticated request is
denied"*). To match:

1. Scan `tasks.md`'s "Tests" section(s) for a task whose described
   input/action and expected outcome matches the scenario's `WHEN`/`THEN`.
2. If found, open the test file that task's section header names
   (a component test or end-to-end spec — the project's checks say which kinds
   of test files exist) and locate the actual
   test — don't just trust the task description;
   read the test body and confirm it really asserts the `THEN` outcome, not
   something adjacent to it.
3. Record the test's fully-qualified name (test file + test
   description) as the evidence.

### 4.2 A pre-existing test not written specifically for this change

Sometimes a scenario is already covered by a broader existing test (e.g. a
general auth-guard test that happens to also cover the route this change
added). Search the test files for the component/service/behavior the
scenario describes even if `tasks.md` didn't call out a dedicated test
task for it.

### 4.3 Manual verification (fallback)

For scenarios with no automated coverage — commonly true-visual claims
("the task card SHALL show a checkbox for marking it done") or scenarios
genuinely awkward to unit-test — perform the check directly, following the
project's manual-verification recipes (how to start a running instance,
issue a real request, or drive the UI), and compare the real result to the
scenario's `THEN`.

- Record exactly what was run and what was observed as the evidence — not
  "looks correct," a specific command/action and its specific output.

### 4.4 No evidence found

If none of the above produces evidence, the scenario is **unverified** —
say so plainly rather than inferring it's probably fine because the
surrounding code looks reasonable. An unverified scenario is not the same
as a failing one; don't conflate the two in the report (§6).

## 5. Verify the Evidence Actually Holds

Matching a test to a scenario isn't the same as confirming it passes *now*:

- For a matched automated test (§4.1/§4.2), **run it** — the single spec
  file (the project's checks say how) if the runner supports that granularity,
  otherwise
  the narrowest containing suite. A test that exists but is currently
  failing, skipped, or was silently broken by a later refactor is a
  compliance gap, not a pass — report it as failing, not as verified.
- For a manual check (§4.3), the check **is** the verification — there's
  nothing further to run, but be honest if the check was only partially
  performed (e.g. you confirmed the happy path but not the exact error
  the scenario specifies for the denied case).

## 6. Report

Structure the in-chat summary as follows, one row per scenario:

```
## Spec Compliance: <change-name>

### ✅ Verified (<N>)
- **<Requirement title>** → *<Scenario title>* — `<test file> > it('...')` (passing)
- **<Requirement title>** → *<Scenario title>* — manual check: signed in as a second test account → <observed result matched THEN>

### ❌ Failing (<N>)
- **<Requirement title>** → *<Scenario title>* — `<test file> > it('...')` currently fails: <why>
- **<Requirement title>** → *<Scenario title>* — manual check: <what was run> → <observed result, and how it diverges from THEN>

### ⚠️ Unverified (<N>)
- **<Requirement title>** → *<Scenario title>* — no automated test found, not manually checked

### Verdict
<One or two sentences: is this change ready to archive? A change with any
❌ Failing scenario is not ready — say so directly rather than softening
it. A change with only ⚠️ Unverified scenarios is a judgment call for the
user: recommend closing the gap (write the missing test, or perform and
record the manual check) before archiving, but don't block it
unilaterally — that decision belongs to the user, not this skill.>
```

If there are no findings in a category, omit that category entirely (same
convention as the project's other reviewer skills).

### Acceptance criteria and Definition of Done

When the feature's brief (`<backlog>/features/NNN-<name>.md`) is written as a user story, two more checks come with the scenarios:

- **Acceptance criteria.** Every `- ACn:` line of the brief must be covered by at least one scenario of the change's specs (or, in a project without OpenSpec, by an `ACCEPTANCE:` line of the implementation hand-back). An AC with no scenario and no evidence is listed under ⚠️ Unverified as *AC n — <text>* and counts in the `Unverified` total like any scenario; one whose evidence currently fails is ❌ Failing.
- **Definition of Done.** Read `<backlog>/definition-of-done.md` and the brief's own `## Definition of Done` section, if either exists. Add a `### Definition of Done` block to the report with each item marked met, unmet or not checkable yet, with the evidence for those it can see (a passing suite, a clean build, a report file). Items that depend on a later phase (docs synced, feature archived) are marked "not checkable yet", not unmet. This block is advisory: it never changes the `Verified · Failing · Unverified` totals, and a missing file is not an error.

### Write the report to a dated file

`<reports>/spec-compliance/SPEC_COMPLIANCE_{change-name}_{YYYY-MM-DD}.md`
(repo root, gitignored — a local working artifact, not committed history,
same convention as `<reports>/code-review/`,
`<reports>/security-review/`, `<reports>/database-review/`). If a
file for that change+date already exists,
append `-2`, `-3`, etc. rather than overwriting.

The report's first line is the stamp `<!-- backlog-kit: spec-compliance-check -->`, written by this skill and by no other route: the orchestrator of a pipeline reads it to confirm the report came from this skill and not from a hand-written substitute.
The report states, per scenario, the test or manual check that proves it, and ends with a summary line `Verified N · Failing N · Unverified N` that the orchestrator reads.

---

## Guardrails

- **Never edit anything under `openspec/`** — not `spec.md`, not
  `tasks.md`, not `proposal.md`, not `design.md`. This skill verifies; it
  doesn't correct the spec to match the code or vice versa. If a scenario
  reveals the spec itself was wrong or ambiguous, say so in the report and
  let the user decide whether to fix the code or amend the spec through
  the normal `openspec-*` workflow.
- **Never invoke `openspec-archive-change`** as part of this skill, and
  never suggest the archive happened — this skill's output is an input to
  that decision, not a replacement for it.
- **Never modify the `openspec-*` skills.** If something about this
  skill's workflow would be better served by a change to
  `openspec-archive-change` itself (e.g. eventually calling this skill as
  a built-in gate), that's a decision for the user to make explicitly, not
  something to do unprompted from here.
- **Running a test is read-only** — this
  skill never writes application code, test code, or fixtures to make a
  failing/missing scenario pass. Finding a gap is the deliverable; closing
  it is separate follow-up work.
- **Don't infer verification from code review alone.** "The code looks
  like it would satisfy this scenario" is not evidence — either an
  actually-passing test or an actually-performed manual check is required
  before marking a scenario ✅ Verified.
- **Never run `git commit`, `git push` or open a PR.**
