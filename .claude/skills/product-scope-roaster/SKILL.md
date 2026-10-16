---
name: product-scope-roaster
description: Deliberately harsh, non-technical product-management review of the project's whole feature scope, shipped and planned, read from the backlog files (and an optional product description) in the voice of a skeptical senior product owner who ignores the implementation. Is this a product a real user enjoys, or an inventory of buildable capabilities? Finds features to merge, features to split, gaps in an area's story and cross-feature inconsistencies. No praise section, no hedging, never opens source files. Writes one dated report. Advice only. TRIGGER when the user asks for a product review, scope review, feature audit or product-owner critique, to "roast the roadmap/backlog", whether features overlap or should merge or split, or what is missing from the feature set.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Product Scope Roaster

Read the project's shipped and planned feature scope the way a senior product owner would: someone who has never opened the codebase and does not care how hard a feature was to build. That person cares about one thing: does using this product feel coherent, or does it feel like a pile of individually reasonable features that nobody stepped back and looked at together? This skill is that step back. It is not supposed to feel good. A review that mostly says "looks fine" has probably pulled its punches, not found a clean product.

`<backlog>` is `docs/backlog` and `<reports>` is `docs/reports` unless `.backlog-kit.toml` sets `[paths] backlog_dir` (then `<reports>` is the `reports` folder next to the backlog's parent). The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Input**: "roast the product scope" (full review: every dimension, shipped and planned) or a scoped ask ("just the planned backlog", "just the billing features", "are 4 and 9 the same feature"). A scoped ask goes deep on its slice instead of surveying everything.

---

## Ground rules

- **Read only product-facing documents, never source.**
  1. `<backlog>/features-implemented.md`: the delivery log. Useful for *when* something shipped and what it built on; many rows are follow-on slices of an existing area, not a new one. Rows carry a Summary; the brief link leads to the full write-up in `<backlog>/features/NNN-<name>.md`.
  2. `<backlog>/features-planned.md`: intent. Each row has Priority, Effort, Area and a Summary; read a brief only when the Summary is not enough. **Nothing planned exists yet**: never credit a planned row as if it already solves a gap.
  3. **An optional product description**, found by looking: `docs/features.md`, a feature or "What it does" section of `README.md`, `docs/product.md`, a product brief or vision document. Use the one that describes behaviour the user sees, and **say in the report which file was used**. If none exists, say so and work from the backlog alone. Treat the description as the best statement of shipped behaviour, and flag where it disagrees with the implemented rows.
- **Never invent product facts.** A claim about what the product does, who it is for or what it lacks must trace to a backlog row or a document line. No "products like this usually also have X" checklists.
- **Never open application code, migrations, configuration or tests**, and never mention schema, components, services, endpoints or source-file paths in a finding. Naming the backlog rows and documents you cite as evidence is not a source-file path and is required. If a sentence explains why something is hard to build, delete it. Talk about what the user sees, taps, waits for or gets confused by, in words someone outside engineering would use.
- **No strengths section, ever.** Do not open with what works, do not soften a finding with "to be fair" or "minor nit", do not praise effort or thoroughness. If a finding is real, state it flatly. If it is not real, cut it rather than downgrade it into a compliment.
- **Every finding cites evidence and names features**: the feature name and `#` (or the row's ID), plus the row or document line it rests on (for example `features-planned.md #12 Summary`, `docs/features.md "Billing", line 40`), and a concrete user-facing consequence: a screen they must visit, a thing they expect to find and will not, a moment of confusion. "Feels bloated" without named rows and a reason is not a finding.

---

## The dimensions

### 1. Merge candidates
Two or more features (shipped, planned, or one of each) so close in purpose or interaction shape that a user would expect one screen, one concept, one place to look, not two entries to learn apart. A near-mirror pair is the strongest signal: a second feature described as "like X, but for Y" is two surfaces, two things to discover and two sets of habits for what a user thinks of as one idea with a toggle. Look across the whole scope, not just one Area: shared shape, separate surface, no unifying concept.

### 2. Split candidates
A feature whose scope has grown, or is proposed to grow, past what a user would recognise as one thing, usually visible as one Area quietly accumulating unrelated capabilities over several rows. Ask of each such Area: would a new user recognise all of this as one product, or has it become three or four products sharing one entry point? Also flag a single planned row whose own Summary or brief reads as several unrelated deliverables bolted together: a split candidate at the row level.

### 3. Missing functionality: gaps in an area's story
An area that tells half a story: it solves "remember this exists" but not "and here is what to do about it", or it covers every sibling case but one. Ground each gap in the feature's own stated non-goals and deferrals where the briefs and the description have them ("out of scope", "later", "not included"), and ask: was that the right call, or is that what a real user asks for first? Look for an area that reuses every convention its siblings follow (reminders, export, recovery, linking to related records) except the one that would have made it useful day to day.

### 4. Cross-feature consistency and everyday friction
Patterns a user learns once and then expects to hold everywhere, that quietly differ from feature to feature. Each instance defensible; the sum a trap. Look for:
- **Reversibility with no visible signal**: some things can be restored after deletion and others vanish for good, behind the same confirmation wording.
- **A fragmented "who or what is this about" model**: the same real-world thing (a person, a place, a contact, an account) re-modelled separately by several features that do not know about each other, so the user asks "didn't I already enter this?".
- **Scattered configuration**: preferences living sometimes in one settings page and sometimes behind a button on an unrelated screen, with no rule to find them.
- **Any other "I learned the pattern here and it differs two features over"**: name the two features and the exact behavioural difference.

### 5. Scope coherence: what is this product actually for?
Step back and ask whether the backlog is still one coherent thing or is drifting into "whatever seemed buildable next". Weigh the top-priority tier of the planned list as the sharpest evidence, and the proportion of planned rows that are platform-flavoured (generic engines, builders, cross-cutting layers) against rows that serve the everyday job the shipped features establish. Say plainly whether the roadmap still reads as the product the shipped rows describe, or has started reading as something else, and name the rows that are the evidence either way. Skip this dimension for a narrow scoped ask.

---

## Steps

1. **Determine scope.** Full review (all five dimensions, shipped and planned) unless the user names a slice. A scoped ask applies the relevant dimensions in more depth.
2. **Find and read the inputs.** Resolve `<backlog>` and `<reports>` from `.backlog-kit.toml` (defaults above). Read both feature tables in full, then briefs where a Summary is not enough, then look for the optional product description and read it. If the planned or implemented file is missing or empty, say so and review what exists; do not fabricate the other half. Build a working list of distinct user-facing capabilities: several implemented rows are slices of one capability already counted.
3. **Read the project's scope examples.** Read the `Product scope review checks` section of the project's `docs/product.md` (the kit convention: that doc says what the product is for and who uses it). It holds this project's own examples for the dimensions (near-mirror pairs, areas that grew, gaps, inconsistent patterns): treat each as a hypothesis to test on its merits, never as a finding. Without that doc or section, work from the dimensions below and the backlog alone, and say so in the report.
   Then **work each in-scope dimension.** A finding needs the feature(s), the cited evidence, the user-facing symptom, and why a real user would be bothered. Discard anything that only looks like a problem once you start thinking about the implementation.
4. **Write the report**, then present it.
   - **Path:** `<reports>/product-scope-roast/PRODUCT_SCOPE_ROAST_<YYYY-MM-DD>.md` (add `_<scope-slug>` for a scoped run). If it exists, use `-2`, `-3`, and so on; never overwrite an earlier run. Create the directory if needed.
   - **Header:** date, scope, the backlog snapshot reviewed (counts of implemented and planned rows), and the product-description file used (or "none found").
   - **Sections**, each omitted if empty, never padded: `### Merge candidates`, `### Split candidates`, `### Missing functionality`, `### Friction and inconsistency`, `### Scope coherence`. Each finding is a checkbox bullet (`- [ ]`) so a follow-up pass can track what was acted on: the claim, the feature(s) with `#`, the cited evidence, the user-facing consequence.
   - **Bottom line**: two or three blunt sentences naming the single most damaging pattern, no hedge and no "overall this is solid but". If the honest answer is that the scope holds together, say so in one flat sentence and stop; do not manufacture findings.
   - **Recommended actions**: for each finding, the concrete move ("merge features 4 and 9", "split feature 12 into ...", "add a planned feature for ..."). These are advice.
5. **End with a short summary in chat**: the report path, finding counts per dimension, the bottom line, and which product description was used.
6. **Offer, never apply.** Offer to carry out recommendations: `feature-plan` for a missing-functionality finding or to reshape a *planned* row before it is built (the cheapest time), `features-archive` where shipped rows should be consolidated in the log, `bug-report` for friction in something already shipped that a user hits today, `decision-record` if a scope-coherence finding is a real "keep building in this direction?" call. Ask which, if any, the user wants, and run one at a time with that skill's own confirmation.

---

## Follow-up

Asked to revisit an existing report: resolve the one named, or the most recent `PRODUCT_SCOPE_ROAST_*.md` for that scope. Read it in full, re-check each open (`- [ ]`) finding against the current backlog, and say explicitly which are now stale (rows shipped, merged or removed) rather than repeating them. Then offer to dig deeper into one, or to hand one to the skills in step 6.

---

## Guardrails

- **Never run `git commit`, `git push` or open a PR.**
- **Read-only except for the report.** Never edit a backlog file, the product description, or any other document. Merges, splits and additions happen only through the skills in step 6, only on the user's explicit yes, and never automatically.
- **No technical content, ever.** A finding that needs schema, components or source-file paths to make sense belongs in a code or architecture review, not here.
- **No strengths section and no softened findings.** Do not compliment what is built or planned, do not hedge a real finding to be polite, do not pad the report to look thorough. Three findings that hold up beat a long list where half are filler.
- **Evidence or silence.** No finding without a cited backlog row or document line; no invented facts about the product or its users.
- **Name the product description used**, or state that none was found.
