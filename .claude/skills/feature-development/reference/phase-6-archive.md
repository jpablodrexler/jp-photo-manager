# Feature Development — Phase 6 — Archive

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 6 — Archive (Subagent 6)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (the Final Summary cannot be displayed until
this subagent returns `ARCHIVE: DONE` — see the foreground guardrail
below), with the following prompt:

> Perform these steps in sequence using the Skill tool:
>
> **Step 1 — Verify spec compliance and close every coverage gap.** If the
> project uses OpenSpec (an `openspec/` folder exists) and the
> `spec-compliance-check` skill is installed, invoke it for `<change-name>`.
> Otherwise do the same yourself, with the feature's brief
> (`<backlog>/features/NNN-<change-name>.md`) and the `ACCEPTANCE:` lines
> Phase 2 returned as the spec: list every requirement and report each as
> Verified (a test that currently passes, or a manual check you performed),
> Failing, or Unverified, with the same rules below. (The skill reads
> `openspec/changes/<change-name>/specs/**/spec.md` and `tasks.md`
> (unmodified — this skill is read-only against `openspec/`) and reports
> each acceptance scenario as Verified, Failing, or Unverified; it does not
> invoke `openspec-archive-change` itself and does not edit any files.
>
> - **If the report has any Failing scenario:** stop immediately — do not
>   proceed to Step 2. End your response with
>   `ARCHIVE_BLOCKED: spec-compliance-check found <N> failing scenario(s) — <one-line summary>`.
>   A Failing scenario is a real behavioral gap, not a coverage gap — it
>   belongs back in Phase 2's implementation/review loop, not fixed here.
> - **If the report has any Unverified scenario (no Failing ones): close
>   every one of them before proceeding — never carry an Unverified
>   scenario forward as a caveat.** An Unverified scenario means the
>   behavior may well be correct, but nothing actually proves it — that gap
>   gets closed now, not documented and left open. For each Unverified
>   scenario:
>   1. Read the report's explanation of exactly what's missing (e.g. "no
>      test exercises this compound claim," "no test covers the X branch,"
>      "no mounted UI test asserts Y is absent").
>   2. Add the missing test coverage directly — a new test or
>      assertion extending the relevant existing test file (preferred over a
>      new file, matching this project's test conventions), at whichever layer
>      the gap sits.
>   3. Re-invoke `spec-compliance-check` for `<change-name>` to confirm the
>      scenario now reports Verified.
>   4. Repeat for every Unverified scenario from the original report.
>   - **If closing a gap's test fails** (the scenario turns out to be
>     genuinely broken, not just untested): treat this exactly like a
>     Failing scenario — stop and end your response with
>     `ARCHIVE_BLOCKED: spec-compliance-check found <N> failing scenario(s) — <one-line summary>`.
>     A coverage gap that turns out to hide a real bug is not something to
>     paper over by weakening or dropping the new test.
>   - **If after 3 rounds of add-test-and-re-check a scenario still isn't
>     Verified** and it's not a genuine behavioral failure either (e.g. you
>     cannot find any way to exercise it with this project's test tooling):
>     end your response with `ARCHIVE_BLOCKED — repeated spec-compliance
>     cycles, <N> scenario(s) still unverified — <one-line summary>` rather
>     than silently archiving with a gap. This needs a human judgment call
>     on the testing approach, not indefinite auto-retry.
>   - Once every scenario reports Verified, run the project's tests
>     once more (the commands its project phases use) to confirm the newly added tests pass alongside
>     everything else, then continue to Step 2.
> - **If everything is already Verified:** continue to Step 2 with nothing
>   to close.
>
> **Step 2** — If the `docs-sync` skill is installed, invoke it in its scoped-sync mode for the
> current branch's changes (it diffs against the branch's base itself — no
> need to pass a file list); otherwise check yourself whether the change makes
> anything the project documents stale (a command, a configuration option, a
> route, the schema, setup steps) and update those passages briefly. This keeps the project's reference docs from
> drifting behind whatever this feature just shipped. This step is **best-effort,
> not blocking**: if the skill reports nothing to sync, continue to Step 3
> regardless. Only stop and surface details to the user if the skill
> itself errors out in a way that leaves files in a broken state.
>
> **Step 3** — OpenSpec projects only. The change's delta specs must reach the
> main specs through the `openspec-sync-specs` skill — never by editing
> `openspec/specs/**` by hand, and never skipped.
> 1. If `openspec/changes/<change-name>/specs/` holds any delta spec, invoke
>    `openspec-sync-specs <change-name>` via the Skill tool first and wait for it
>    to finish. A hand-written merge is not a substitute, even for an
>    ADDED-only delta.
> 2. Then invoke `openspec-archive-change <change-name>` and wait for it to
>    complete fully (the SDD change directory must be moved to
>    `openspec/changes/archive/`). When it offers the delta spec sync, answer
>    "Sync now" (or "Archive now" if step 1 already synced and it reports the
>    specs as already in sync) — never "Archive without syncing". Answer its
>    incomplete-task prompts normally.
> 3. Report in your hand-back whether `openspec-sync-specs` ran
>    (`SPECS SYNCED: via openspec-sync-specs` or `SPECS SYNCED: n/a — no delta
>    specs`). If you could not run the skill, end your response with
>    `ARCHIVE_BLOCKED — openspec-sync-specs unavailable` instead of merging by
>    hand.
>
> **Step 4** — Invoke `features-archive <change-name>`. Wait for it to
> complete fully (the feature row must be moved from the planned table in
> `<backlog>/features-planned.md` to the implemented table in
> `<backlog>/features-implemented.md`, with its `Details` cell linking
> the brief file and the archived spec).
>
> After all four steps complete, end your response with exactly this line:
> `ARCHIVE: DONE`

Do not display the Final Summary until this subagent returns `ARCHIVE:
DONE` or `ARCHIVE_BLOCKED`. An `ARCHIVE_BLOCKED` result means the feature is
implemented and tested but **not archived** — surface the failing/unverified
scenario(s) to the user and wait for guidance rather than proceeding or
retrying automatically. This workflow never archives a change with a known
spec-compliance gap: Step 1 closes every Unverified scenario with real test
coverage (or escalates via `ARCHIVE_BLOCKED` if it genuinely can't) before
Step 2 ever runs — there is no "archive now, note the gap for later" path.

---
