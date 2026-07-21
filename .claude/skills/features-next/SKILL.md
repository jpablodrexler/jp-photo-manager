---
name: features-next
description: Recommends which feature to implement next based on openspec/features.md priorities and dependencies, then asks for user confirmation. Returns the confirmed change name to the caller — does NOT invoke opsx:propose or opsx:apply. TRIGGER when the user asks which feature to implement next, what to work on next, or which feature to suggest — including phrases like "recommend the next feature", "suggest the next feature", "what feature should we do next", or any similar request for a next-step recommendation from the features list.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.2"
---

Recommend the next feature to implement and return the confirmed change name to the caller.

**Input**: Optional feature number or name to skip the recommendation step and jump straight to confirmation.

---

## Steps

### 1. Read features.md

Read `openspec/features.md` in full.

### 2. Collect pending features

From the `## Feature List` table, collect every row whose **Implementation** column shows `⬜ Pending`. For each row record:

- `number` — the `#` column value (integer)
- `name` — the backtick-wrapped value in the **Change name** column (e.g. `image-etag-cache`)
- `artifacts_ready` — `true` if the **Artifacts** column shows `✅ Created`, `false` if `⬜ Pending`
- `brief` — the **Brief description** column text

### 3. Determine which features are unblocked

A feature is **blocked** if it has a hard dependency on another feature that is still `⬜ Pending` in the Implementation column. Derive this from the **Hard implementation dependencies** sub-section of `## Dependencies`.

Build a blocked set: for each hard-dependency statement `A → B`, if B's Implementation is still `⬜ Pending`, mark A as blocked.

A feature is **unblocked** if it is not in the blocked set.

### 4. Score and rank unblocked features

Apply the following scoring rules in order (higher score = higher priority):

**Tier 1 — Explicit priority labels (from the `## Dependencies` section)**

Scan for the `P0 — production-safety gaps`, `P1 — high-impact`, `P2 — scalability`, `P3 — operational convenience` subsections. Assign:
- P0 → score 400
- P1 → score 300
- P2 → score 200
- P3 → score 100
- No explicit tier → score 50

**Tier 2 — Artifacts ready bonus**

Add +20 if `artifacts_ready` is `true` (SDD already created, can apply immediately without the propose step).

**Tier 3 — Recommended implementation order bonus**

The `### Recommended implementation order` subsection lists features in suggested sequence. Add +10 for features that appear in that list (they have been explicitly ordered).

**Tier 4 — No schema change bonus**

Features noted in `## Dependencies` implementation notes as "no schema change" or "no Flyway migration" are simpler to deliver. Add +5 for these.

Select the highest-scoring unblocked feature. Break ties by choosing the lower feature number (smaller = older = likely simpler).

### 5. Present the recommendation

Display:

```
## Recommended Next Feature

**#<number> `<name>`**
<brief description>

**Why this feature:**
- <tier reason, e.g. "P0 production-safety gap" or "unblocked, artifacts already created">
- <dependency status, e.g. "all prerequisites implemented">
- <any relevant note from implementation notes>

**Artifacts:** <✅ Created / ⬜ Not yet created>
```

Also show the top 3 runners-up (by score) with a one-line reason each so the user can override.

### 6. Ask for user confirmation

`AskUserQuestion` only accepts 2–4 options per question — never enumerate the full unblocked list through it. With a large backlog (this repo's `features.md` currently has dozens of unblocked pending features), only the recommendation plus its top few runners-up fit; anything beyond that must be a free-text answer.

Use the **AskUserQuestion tool** with a single question:

- **Question**: "Proceed with implementing `<name>`?"
- **Options** (at most 4 total — the recommended feature plus up to 3 runners-up from step 5; if step 5 found fewer than 3 runners-up, that's fine, just use however many exist):
  1. "Yes, proceed with `<name>`" — confirm the recommended feature
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

- Always read the full `features.md` before scoring — never guess which feature is next from memory.
- Do not invoke `opsx:apply`, `opsx:propose`, or any other skill. This skill's sole responsibility is recommendation, confirmation, and returning the change name. The caller decides what to do next.
- **Resolving a feature by number or name**: whenever a specific feature number or name is available — whether it's the skill's initial input, or a free-text answer typed into the "Other" option of step 6's `AskUserQuestion` — look it up directly as the row in `features.md` whose `#` or `Change name` column matches, rather than re-running the scoring pass. If it's the skill's initial input, skip steps 2–5 and go directly to step 6 with that feature pre-selected (but still confirm). If it's a step-6 "Other" answer, treat it as the user's final selection (re-confirming isn't necessary — they already answered the confirmation question).
- A feature already showing `✅ Implemented` in the Implementation column must never be recommended.
- If ALL unblocked features have `artifacts_ready = false`, the recommendation will still be returned — the caller handles artifact creation.
- If ALL pending features are blocked, inform the user which blocking prerequisites need to be implemented first, and surface those prerequisites as the recommendation instead.
