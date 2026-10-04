# Feature Development — Phase 4 — E2E Verification

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 4 — E2E Verification (Subagent 5, conditional)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 5 cannot start until this subagent
reports its signal — see the foreground guardrail in `../SKILL.md`), with the
following prompt:

> Perform these steps in sequence.
>
> **Step 1 — Determine whether E2E verification applies to this change**
> Run `git status --porcelain` and parse every line's file path — including
> untracked (`??`) entries — into a concrete list; call it `CHANGED_FILES`.
> This is the same recompute-fresh convention Phase 2 uses — never assume a
> list from an earlier phase is still accurate.
>
> Check whether any path in `CHANGED_FILES` falls under one of these
> UI-facing/data areas: authentication (`core/guards/auth.guard.ts`,
> `core/interceptors/auth.interceptor.ts`, the `features/auth/login`
> component), the gallery (`features/gallery/`, the app's primary
> photo-browsing flow), or a Flyway migration under `db/migration/` (a
> schema change can silently change what any of the above renders, or what
> the backend serves, even with no frontend file touched).
>
> - If **none** of these areas were touched: end your response with exactly
>   `E2E: SKIPPED — no auth/gallery/migration changes in this change` and
>   stop. Do not start the backend/frontend or run Cypress for a change
>   that doesn't touch any of these areas.
> - If **any** were touched: continue to Step 2.
>
> **Step 2 — Confirm prerequisites are ready**
> Follow `e2e-testing` §1 (PostgreSQL, MongoDB, Redis, and — only if this
> change touches sync/convert/duplicate-detection or another
> Kafka-consuming flow — Kafka; §1.5's check that real data exists). If any
> required prerequisite isn't reachable, end your response with
> `E2E_BLOCKED — <brief reason>` and stop — do not attempt to fix an
> environment/infrastructure problem yourself.
>
> **Step 3 — Start the backend and frontend**
> Follow `e2e-testing` §2 (start the backend) and §3 (start the frontend).
> If either never comes up, end your response with `E2E_BLOCKED — backend/
> frontend did not start` and stop.
>
> **Step 4 — Run the browser checks**
> 1. Authenticate per `e2e-testing` §4 (the fixed `admin`/`admin`
>    credential — see §4.1 for the BCrypt-reset fallback if login returns
>    401). Confirm the session reaches `/home` and the dashboard renders.
> 2. Follow `e2e-testing` §7 (visual verification via a throwaway Cypress
>    spec) for whichever surface this change touched. If this change
>    touched the gallery specifically, also exercise its relevant flow
>    (thumbnail grid load, viewer mode, or whichever interaction changed)
>    once through the actual UI — not just confirming the page loads — to
>    confirm the new/changed behavior works against the real backend.
> 3. Follow `e2e-testing` §8 (interactive navigation) to confirm routing
>    between the touched area and at least one neighboring route works.
> 4. Take a screenshot at the final state and read it back to confirm
>    there's no visible layout break.
>
> **Step 5 — Tear down**
> Follow `e2e-testing` §10 (stop the backend/frontend, and any infra this
> run started that isn't part of the developer's normal always-on setup).
>
> End your response with one of:
>
> - `E2E: PASS — <one-line summary of what was exercised>` if every check in
>   Step 4 passed.
> - `E2E_BLOCKED — <brief reason>` if any check failed, a prerequisite
>   wasn't ready, or a console error/visible layout break was observed.

Do not start Phase 5 until this subagent reports `E2E: PASS` or
`E2E: SKIPPED`. If it reports `E2E_BLOCKED`, surface the details to the
user and wait for guidance before continuing — a failure here is against
locally running infrastructure, so it may equally be an environment/data
problem as a code defect (see `e2e-testing`'s own framing); don't assume
it belongs back in Phase 2's code review without the user confirming that.

---
