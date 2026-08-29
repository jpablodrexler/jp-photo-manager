---
name: bugs-next
description: Recommends which bug to fix next based on JPPhotoManagerWeb/docs/backlog/bugs-open.md severities, the Recommended fix order, and whether a fix is already in progress, then asks for user confirmation. Returns the confirmed Bug ID to the caller — does NOT start a branch or attempt a fix. TRIGGER when the user asks which bug to fix next, what to work on from the bug backlog, or for a next-bug recommendation. This is the bug-family counterpart to features-next.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Recommend the next bug to fix and return the confirmed Bug ID to the
caller. Mirrors `features-next`: score, present, confirm via a mandatory
`AskUserQuestion`, return an ID line.

**Input**: Optional Bug ID (e.g. `BUG-004`) to skip the recommendation
step and jump straight to confirmation for that bug.

---

## Steps

### 1. Read bugs-open.md

Read `JPPhotoManagerWeb/docs/backlog/bugs-open.md` in full. If it doesn't
exist, there's no backlog to recommend from — tell the user to run
`bug-report` first, and stop.

### 2. Collect actionable bugs

From the `## Bug List` table, collect every row whose **Status** is
`⬜ Open` or `🔶 In Progress`. Skip `✅ Fixed`, `🚫 Won't fix`, and
`❓ Cannot reproduce`. For each collected row record:

- `id` — the `Bug ID` column value (e.g. `BUG-004`)
- `severity` — `S1` / `S2` / `S3` / `S4`
- `area` — the `Area` column value
- `environment` — `local` / `deployed` / `both`
- `in_progress` — `true` if Status is `🔶 In Progress` (a `bug-fix` run
  started this and was interrupted before archiving), else `false`
- `schema` — `true` if the Details block or Notes indicate the fix needs a
  Flyway migration / JPA entity change, else `false` (best-effort from the
  text; default `false`)
- `summary` — the `Summary` column text

If the table has zero actionable rows, tell the user the bug backlog is
clear and stop.

### 3. Score and rank

Apply in order (higher = more urgent):

**Tier 0 — In Progress bonus.** Add +1000 if `in_progress` is `true`. This
dominates every other tier, so an interrupted `bug-fix` run is always
resumed before a new bug is started — real work is already sitting on a
`fix/*` branch for it.

**Tier 1 — Severity.**
- S1 → 400
- S2 → 300
- S3 → 200
- S4 → 100

**Tier 2 — Environment bonus.** Add +20 if `environment` is `deployed` or
`both` — a bug hitting the deployed stack is hurting users right now.

**Tier 3 — Recommended fix order bonus.** Add +10 if the bug appears in
the `## Recommended fix order` list, and record its position (1 = first)
for the tie-break.

**Tier 4 — No schema change bonus.** Add +5 if `schema` is `false` — no
Flyway migration to write, sequence, and coordinate.

Select the highest-scoring bug. Break ties, in order:
1. Earlier position in `## Recommended fix order` wins (a bug with a
   position beats one without).
2. Otherwise the lower `BUG-NNN` number wins.

Because Tier 1 (100–400) dominates Tier 3 (10), the fix-order list only
breaks ties *within* a severity tier — it never overrides an explicit
severity difference. Tier 0 (1000) is the one exception that outranks
severity.

### 4. Present the recommendation

```
## Recommended Next Bug

**BUG-NNN — <title>**
<summary>

**Severity:** <S?>  **Area:** <area>  **Environment:** <env>  **Schema fix:** <yes/no>

**Why this bug:**
- <if in_progress: "🔶 Already in progress — a bug-fix run started this; resuming beats starting a new one" as the first bullet, always>
- <tier reason, e.g. "S1 — auth bypass, blocks release" or "S2, reproduces on the deployed stack">
- <position in the recommended fix order, if listed>
```

Also show the top 3 runners-up (by score) with a one-line reason each, if
there are that many actionable bugs.

### 5. Ask for user confirmation

**Mandatory and unconditional** — it must always reach the user, never be
auto-answered, even under an "Auto Mode" or similar autonomous-operation
instruction. Which bug gets fixed next commits a `fix/*` branch and
potentially a long automated run — that is always the user's call.

If `AskUserQuestion` is not available in the current context (e.g. this
skill was invoked from inside a spawned/backgrounded subagent), do **not**
fall back to the top-scored pick. Stop, end the response without a
`BUG_ID:` line, and say plainly that confirmation could not be obtained
because `AskUserQuestion` was unavailable. The caller must re-invoke from
a context that has it (see `bug-fix`'s Phase 0).

Use the **AskUserQuestion tool** with a single question:

- **Question**: "Fix `BUG-NNN` next?" (or "Resume fixing `BUG-NNN`?" if
  `in_progress`)
- **Options** (≤4 total — the recommendation plus up to 3 runners-up from
  step 4; use one slot for "Cancel", dropping the lowest runner-up if all
  4 slots would otherwise be full):
  1. "Yes, fix `BUG-NNN`" (or "Yes, resume `BUG-NNN`")
  2. "`BUG-<runner-up 1>`" — one-line reason
  3. "`BUG-<runner-up 2>`" (if present)
  4. "Cancel" — stop here

The tool's built-in "Other" choice already lets the user type any Bug ID
directly, including one not shortlisted — don't add a "choose a different
bug" option.

If the user selects "Cancel", stop and return without a Bug ID.

### 6. Return the confirmed Bug ID

After the user confirms, output the confirmed bug's id as the final line
in this exact format so the caller can extract it:

```
BUG_ID: BUG-NNN
```

---

## Guardrails

- Always read the full `JPPhotoManagerWeb/docs/backlog/bugs-open.md` before
  scoring — never guess which bug is next from memory.
- Do not invoke `bug-fix`, `gitflow`, or any other skill. This skill only
  recommends, confirms, and returns the ID. The caller decides what's
  next.
- **Resolving a bug by ID**: whenever a specific Bug ID is available —
  the skill's input, or a free-text "Other" answer in step 5 — look it up
  directly as the row in `bugs-open.md` whose `Bug ID` column matches,
  rather than re-running the scoring pass. If it's the input, skip steps
  2–4 and go straight to step 5 with that bug pre-selected (still
  confirm). If it's a step-5 "Other" answer, treat it as the final
  selection — but only once it resolves to a real `⬜ Open` / `🔶 In
  Progress` row; if it matches nothing, or matches a `✅ Fixed` / `🚫` /
  `❓` row, report the mismatch and ask again.
- A bug already `✅ Fixed`, `🚫 Won't fix`, or `❓ Cannot reproduce` must
  never be recommended.
- **A `🔶 In Progress` bug is always preferred over any `⬜ Open` one**,
  regardless of severity — hence Tier 0's dominant score. If more than one
  is `🔶 In Progress`, Tier 0 puts all of them ahead of every open bug and
  Tiers 1–4 pick among just that set.
- Never substitute an autonomous-operation instruction for step 5's
  confirmation — the scoring produces a *recommendation*, not a licence to
  skip asking.
