# Feature Development — Phase 6 — Archive

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 6 — Archive (Subagent 7)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (the Final Summary cannot be displayed until this
subagent returns `ARCHIVE: DONE` — see the foreground guardrail in `../SKILL.md`), with
the following prompt:

> Perform these steps in sequence using the Skill tool:
>
> **Step 1 — Verify spec compliance and close every coverage gap.** Invoke
> `spec-compliance-check` for `<change-name>`. It reads
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
>   2. Add the missing test coverage directly — a new backend
>      (`@SpringBootTest` / `@WebMvcTest` / `@DataJpaTest`) test or a
>      Cypress component test/assertion extending the relevant existing
>      spec file (preferred over a new file, matching this project's
>      conventions — see `java-unit-test-developer` /
>      `cypress-unit-test-developer`), or an E2E case if that's the layer
>      the gap sits at.
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
>   - Once every scenario reports Verified, run the affected test suite(s)
>     once more (`cd JPPhotoManagerWeb/backend && mvn test` and/or `cd
>     JPPhotoManagerWeb/frontend && npm test`) to confirm the newly added
>     tests pass alongside everything else, then continue to Step 2.
> - **If everything is already Verified:** continue to Step 2 with nothing
>   to close.
>
> **Step 2** — Invoke `web-docs-sync` in its scoped-sync mode for the
> current branch's changes (it diffs against `origin/develop` itself — no
> need to pass a file list). This keeps `JPPhotoManagerWeb/CLAUDE.md` and
> `JPPhotoManagerWeb/docs/*.md` from drifting behind whatever this feature
> just shipped (a new endpoint, config property, cache, Kafka topic, route,
> deploy-manifest env var, or custom metric). This step is **best-effort,
> not blocking**: if the skill reports nothing to sync, or the diff is
> outside its scope (e.g. a WPF-only or tooling-only change), continue to
> Step 3 regardless. Only stop and surface details to the user if the skill
> itself errors out in a way that leaves files in a broken state.
>
> **Step 3** — Invoke `openspec-archive-change <change-name>`. Wait for it to
> complete fully (the SDD change directory must be moved to
> `openspec/changes/archive/`). This skill may prompt you about delta spec sync
> or incomplete tasks — respond to those prompts normally; they are part of the
> archiving workflow.
>
> **Step 4** — Invoke `features-archive <change-name>`. Wait for it to
> complete fully (the feature's row must be moved out of
> `JPPhotoManagerWeb/docs/backlog/features-planned.md` into
> `JPPhotoManagerWeb/docs/backlog/features-implemented.md` in the
> implemented-table shape — no `Summary`/`Brief`/`Implementation` cells, a
> `Details` cell linking the brief file and the archived spec; the brief
> file under `JPPhotoManagerWeb/docs/backlog/features/` stays untouched).
>
> After all four steps complete, end your response with exactly this line:
> `ARCHIVE: DONE`

Do not display the Final Summary until this subagent returns `ARCHIVE: DONE`
or `ARCHIVE_BLOCKED`. An `ARCHIVE_BLOCKED` result means the feature is
implemented and tested but **not archived** — Step 1's `spec-compliance-check`
found a Failing scenario, a test added to close an Unverified scenario
itself failed (revealing a real bug), or an Unverified scenario survived 3
rounds of add-test-and-re-check. Surface the scenario(s) to the user and
wait for guidance (fix the code, fix the spec via the normal `openspec-*`
workflow, or override and archive manually) rather than proceeding or
retrying automatically. This workflow never archives a change with a known
spec-compliance gap: Step 1 closes every Unverified scenario with real test
coverage (or escalates via `ARCHIVE_BLOCKED` if it genuinely can't) before
Step 2 ever runs — there is no "archive now, note the gap for later" path.

---
