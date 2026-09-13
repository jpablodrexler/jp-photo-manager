---
name: features-next
description: Recommends which feature to implement next based on JPPhotoManagerWeb/docs/backlog/features-planned.md priorities, dependencies, and its Recommended implementation order, then asks for user confirmation. If any in-progress exploration exists under JPPhotoManagerWeb/docs/explorations/, first offers (optionally) to keep exploring one of those ideas instead of picking a feature. Returns the confirmed change name to the caller — does NOT invoke opsx:propose or opsx:apply. TRIGGER when the user asks which feature to implement next, what to work on next, or which feature to suggest — including phrases like "recommend the next feature", "suggest the next feature", "what feature should we do next", or any similar request for a next-step recommendation from the features list.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.5"
---

Recommend the next feature to implement and return the confirmed change name to the caller.

**Input**: Optional feature number or name to skip the recommendation step and jump straight to confirmation.

---

## Steps

### 0. Offer an in-progress exploration first (optional off-ramp)

Before recommending a feature, check whether there's an open exploration the user might want to keep developing instead of starting a new feature.

1. List `JPPhotoManagerWeb/docs/explorations/*.md` (skip a `README.md` if present). If the directory doesn't exist or holds no such files, skip this step entirely and go to step 1.
2. For each file, read its `**Status:**` line near the top and its `# ` H1 title. Treat the exploration as **open** unless the status line marks it finished — i.e. it contains `archived`, `superseded`, `proposed`, `done`, or `dropped`. A file with no `**Status:**` line counts as open. Record each open exploration's title and path.
3. If there are no open explorations, go to step 1 without mentioning this — it's not worth a line of output when there's nothing to offer.
4. If there is at least one open exploration, use the **AskUserQuestion tool** — this is a genuine optional off-ramp and must actually reach the user. If `AskUserQuestion` is unavailable in this context, skip this step and proceed to the normal recommendation (do **not** block on it — unlike step 6, choosing to explore is not a prerequisite for anything):
   - **Question**: "There's an in-progress exploration. Recommend the next feature to implement, or keep working on one of these explorations?"
   - **Options** (at most 4 total):
     1. "Recommend the next feature" — the normal path, and this skill's default job
     2. "`<exploration 1 title>`" — "keep exploring this"
     3. "`<exploration 2 title>`" (if present)
     4. "`<exploration 3 title>`" (only if the total stays at or under 4)
   - If more than 3 open explorations exist, list the 3 most recently modified and let the tool's built-in "Other" cover the rest — resolve a free-text answer to the file whose title or filename matches.
5. If the user picks "Recommend the next feature" (or the tool was unavailable), continue to step 1.
6. If the user picks an exploration, **stop here.** Do not collect or score features, and do not emit a `CHANGE_NAME:` line. Point the user at the exploration's file path and tell them to continue it with `/openspec-explore` (naming the topic). This skill's job is done — the caller treats the absence of a `CHANGE_NAME:` line exactly as it treats a step-6 "Cancel".

### 1. Read features-planned.md

Read `JPPhotoManagerWeb/docs/backlog/features-planned.md` in full.

### 2. Collect pending features

From the `## Feature List` table, collect every row whose **Implementation** column shows `⬜ Pending` or `🔶 In Progress`. For each row record:

- `number` — the `#` column value (integer)
- `name` — the backtick-wrapped value in the **Change name** column (e.g. `image-etag-cache`)
- `priority` — the **Priority** column value (`P0`/`P1`/`P2`/`P3`)
- `schema_change` — the **Schema Change** column value (`Yes`/`No`)
- `effort` — the **Effort** column value (`S`/`M`/`L`)
- `area` — the **Area** column value (`Backend`/`Frontend`/`Full-stack`/`Infra`)
- `artifacts_ready` — `true` if the **SDD Artifacts** column shows `✅ Created`, `false` if `⬜ Pending`
- `in_progress` — `true` if the **Implementation** column shows `🔶 In Progress` (`feature-development` has already started this one — see `feature-development`'s Step 1.6), `false` if `⬜ Pending`
- `brief` — the **Brief description** column text

### 3. Determine which features are unblocked

A feature is **blocked** if it has a hard dependency on another feature that is not yet `✅ Implemented`. Derive this from the **Hard implementation dependencies** sub-section of `## Dependencies`.

Build a blocked set: for each hard-dependency statement `A → B` found there, look up B's row in the `## Feature List` table you just read:
- If B's row shows `⬜ Pending` **or** `🔶 In Progress`, mark A as blocked — a prerequisite that's actively being worked on hasn't shipped yet either, so it blocks exactly like a merely-pending one does. Do not treat "not literally `⬜ Pending`" as "safe to recommend"; the only status that clears a dependency is B actually being done.
- If B has no row at all in the `## Feature List` table, treat the dependency as satisfied (not blocking) — the only way a feature is absent from that table is that `features-archive` already moved it to `features-implemented.md` as `✅ Implemented`. Do not treat a missing row as "unknown, so don't block" by accident; confirm it's genuinely archived (a quick check against `JPPhotoManagerWeb/docs/backlog/features-implemented.md` if there's any doubt) rather than assuming.
- **Never fall back to the dependency statement's own parenthetical annotation** (e.g. "(prerequisite still pending)" / "(prerequisite already implemented)") to decide blocking — that text is a human-readable summary maintained by `features-archive` and can lag the actual Implementation-column status. Always re-derive from B's live row in the Feature List table itself.

A feature is **unblocked** if it is not in the blocked set.

### 4. Score and rank unblocked features

Apply the following scoring rules in order (higher score = higher priority):

**Tier 0 — In Progress bonus**

Add +1000 if `in_progress` is `true`. This dominates every other tier
(worth at most 400+20+10+5 = 435 combined), so a feature `feature-development`
has already started always outranks every merely-pending one, regardless of
priority — resuming existing work takes precedence over starting something
new. See the Guardrails below for why.

**Tier 1 — Priority column**

Read the row's `priority` value directly. Assign:
- P0 → score 400
- P1 → score 300
- P2 → score 200
- P3 → score 100
- Missing/blank → score 50 (treat as untiered rather than erroring)

**Tier 2 — SDD Artifacts ready bonus**

Add +20 if `artifacts_ready` is `true` (SDD already created, can apply immediately without the propose step).

**Tier 3 — Recommended implementation order bonus**

The `### Recommended implementation order` subsection lists features in suggested sequence. Add +10 for features that appear in that list (they have been explicitly ordered), and record each such feature's position (1 = first) for the tie-break below — a feature absent from the list has no position.

**Tier 4 — No schema change bonus**

Add +5 if the row's `schema_change` value is `No` — these are simpler to deliver (no migration to write, sequence, or coordinate with other pending migrations).

Select the highest-scoring unblocked feature. Break ties, in order:
1. Earlier position in the `### Recommended implementation order` list wins (a feature with a position beats one without).
2. If neither tied feature has a position (or both are missing from the list), the lower feature number wins (smaller = older = likely simpler).

Because Tier 1 (priority, worth 100–400 points) dominates Tier 3 (worth 10), the order list only ever breaks ties *within* a priority tier or acts as the final tie-break — it does not override an explicit priority difference between features. That's deliberate: `features-planned.md`'s own Priority column is the primary signal; the order list is a same-priority sequencing aid. Tier 0 (worth 1000) is the one exception that *does* override priority — see Tier 0 above.

### 5. Present the recommendation

Display:

```
## Recommended Next Feature

**#<number> `<name>`**
<brief description>

**Priority:** <priority>  **Effort:** <effort>  **Area:** <area>  **Schema change:** <schema_change>

**Why this feature:**
- <if in_progress: "🔶 Already in progress — feature-development started this one; resuming beats starting something new" as the first bullet, always>
- <tier reason, e.g. "P0 production-safety gap" or "unblocked, artifacts already created">
- <dependency status, e.g. "all prerequisites implemented">
- <position in the Recommended implementation order, if listed, e.g. "sequenced first among the P2s in the recommended order" — omit this bullet if the feature isn't in that list>
- <any relevant note from implementation notes>

**SDD Artifacts:** <✅ Created / ⬜ Not yet created>
```

Also show the top 3 runners-up (by score) with a one-line reason each so the user can override.

### 6. Ask for user confirmation

**This confirmation is mandatory and unconditional — it must always actually reach the user, never be silently skipped or auto-answered.** This holds even when the caller is running under an "Auto Mode" or similar autonomous-operation instruction that biases toward proceeding without stopping to ask: that bias is meant for implementation judgment calls (which helper function to use, how to phrase a commit message), never for which feature gets built. Selecting a feature commits potentially hours of automated work and a real git branch — that is always the user's call, not a "reasonable default" for the model to make on their behalf.

If `AskUserQuestion` is not available in the current execution context — e.g. this skill is being invoked from inside a spawned/backgrounded subagent that has no interactive tool access, rather than the orchestrator's own foreground context — do **not** fall back to picking the top-scored recommendation and proceeding. Stop instead: end the response without a `CHANGE_NAME:` line, and state plainly that confirmation could not be obtained in this context because `AskUserQuestion` was unavailable. The caller is responsible for re-invoking this skill from a context that does have it (see `feature-development`'s Phase 0, which exists specifically so this skill is always invoked from the orchestrator's own context, never from inside a subagent).

`AskUserQuestion` only accepts 2–4 options per question — never enumerate the full unblocked list through it. With a large backlog (this repo's `JPPhotoManagerWeb/docs/backlog/features-planned.md` currently has dozens of unblocked pending features), only the recommendation plus its top few runners-up fit; anything beyond that must be a free-text answer.

Use the **AskUserQuestion tool** with a single question:

- **Question**: "Proceed with implementing `<name>`?" (if `in_progress` is `true`, phrase it as "Resume implementing `<name>`?" instead — it's already underway, not a fresh start)
- **Options** (at most 4 total — the recommended feature plus up to 3 runners-up from step 5; if step 5 found fewer than 3 runners-up, that's fine, just use however many exist):
  1. "Yes, proceed with `<name>`" (or "Yes, resume `<name>`" if `in_progress`) — confirm the recommended feature
  2. "`<runner-up 1 name>`" — one-line reason from step 5
  3. "`<runner-up 2 name>`" (if present)
  4. "`<runner-up 3 name>`" (if present, and only if the total stays at or under 4)
  - "Cancel" — stop here (use one of the slots above for this; drop the lowest-ranked runner-up if all 4 slots would otherwise be full)

Do not add a separate "Choose a different feature" option that tries to show the rest of the list — the tool's built-in "Other" choice already lets the user type any number or name directly, including one that didn't make the shortlist. Resolve that free-text answer per "Resolving a feature by number or name" in the Guardrails below.

If the user selects "Cancel", stop and return without a change name.

### 7. Return the confirmed change name

After the user confirms, output the confirmed feature's change name as the final line of the response in this exact format so the caller can extract it:

```
CHANGE_NAME: <change-name>
```

---

## Guardrails

- Always read the full `JPPhotoManagerWeb/docs/backlog/features-planned.md` before scoring — never guess which feature is next from memory.
- Do not invoke `opsx:apply`, `opsx:propose`, `openspec-explore`, or any other skill. This skill's sole responsibility is recommendation, confirmation, and returning the change name (or, via step 0, surfacing an exploration off-ramp and stopping). The caller decides what to do next.
- **Step 0's exploration off-ramp never returns a `CHANGE_NAME:` line.** If the user chooses to keep exploring, this skill stops with no recommendation; the caller must treat that identically to a step-6 "Cancel" and not proceed to implement anything. Step 0 is also purely additive — a skipped or unavailable step 0 (no `JPPhotoManagerWeb/docs/explorations/`, no open files, or no `AskUserQuestion`) must never block the normal recommendation flow.
- **Resolving a feature by number or name**: whenever a specific feature number or name is available — whether it's the skill's initial input, or a free-text answer typed into the "Other" option of step 6's `AskUserQuestion` — look it up directly as the row in `JPPhotoManagerWeb/docs/backlog/features-planned.md` whose `#` or `Change name` column matches, rather than re-running the scoring pass. If it's the skill's initial input, skip steps 2–5 and go directly to step 6 with that feature pre-selected (but still confirm). If it's a step-6 "Other" answer, treat it as the user's final selection (re-confirming isn't necessary — they already answered the confirmation question) — but only once it resolves to a real row: if the typed value matches no row at all, or matches a row already showing `✅ Implemented` (which must never be recommended, per the guardrail below — a manually-typed answer doesn't get an exception), report the mismatch to the user and ask again rather than returning a `CHANGE_NAME:` for something that isn't actually a valid pending feature.
- A feature already showing `✅ Implemented` in the Implementation column must never be recommended.
- **A feature already showing `🔶 In Progress` is always preferred over any merely `⬜ Pending` feature, regardless of priority.** `feature-development` marks a feature `🔶 In Progress` as soon as it selects it and sets up its branch (see that skill's Step 1.6); if that run was interrupted before archiving, the row is left `🔶 In Progress` with real work already sitting on `feature/<change-name>`. Resuming that work is almost always better than starting a new feature and leaving the in-progress one to rot — hence Tier 0's dominant score. If more than one row happens to show `🔶 In Progress` at once (e.g. two interrupted runs), Tier 0 puts all of them ahead of every pending feature and the normal Tier 1–4 scoring (plus the usual tie-breaks) picks among just that set.
- If ALL unblocked features have `artifacts_ready = false`, the recommendation will still be returned — the caller handles artifact creation.
- If ALL pending features are blocked, inform the user which blocking prerequisites need to be implemented first, and surface those prerequisites as the recommendation instead.
- **A feature with any hard dependency on a prerequisite that isn't `✅ Implemented` must never be recommended — not as the top pick, not as a runner-up.** Step 3's blocked set exists specifically to enforce this; a feature belongs in the blocked set (and out of scoring/step 4 entirely) whenever its prerequisite is `⬜ Pending` or `🔶 In Progress`, full stop, regardless of how attractive its priority/effort/order-list score would otherwise be. Recommending a feature whose backlog-declared dependency hasn't shipped — because a stale annotation was trusted over the live Implementation column, or because "In Progress" was mistaken for "not blocking" — is exactly what this guardrail exists to prevent.
- **Never substitute an autonomous-operation instruction (Auto Mode or similar) for step 6's user confirmation.** No matter how unambiguous the top-scored recommendation looks (e.g. a lone P0/P1 among otherwise P2/P3 candidates), this skill must still ask and wait for an actual answer before returning a `CHANGE_NAME:` line. The scoring in steps 3–4 exists to produce a good *recommendation*, not to make the confirmation optional.
