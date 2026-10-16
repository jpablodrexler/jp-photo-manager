---
name: architecture-reviewer
description: Whole-system architecture health check, qualitative and grounded in the code actually read. Assesses module and folder organisation, layering, dependency direction and cycles, data-access patterns, state and coordination mechanisms (hunting for a unit quietly reused as an undocumented cross-feature channel), error handling, configuration, testing seams, and routing or entry points where they apply. Read-only on source; writes one dated report under the reports folder with severity-marked checkbox findings and file evidence, then ends with a summary. Offers, never automatically, to turn chosen findings into backlog items via feature-plan or bug-report. Distinct from the per-change code review the work skills run. TRIGGER when the user asks for an architecture review or assessment of the whole system, whether the code follows its claimed structure or a named pattern, a deeper look at one architectural dimension, or to work through findings from an earlier ARCHITECTURE_REVIEW report; do not use it to review a single diff.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

Assess the shape of the system as a whole, not a diff. This is the kind of problem a per-change review rarely catches: implicit coupling that builds up one individually reasonable change at a time, where every review saw only its own diff.

The motivating shape: a unit that starts as one narrow concern (a preference, a status flag, a cache hook, one event type) and quietly becomes the "something changed, react" channel for several unrelated features. Nothing in its name says so, and no single change that added one more consumer looked wrong. The same drift happens to a documented dependency rule, eroded one convenient import at a time.

`<reports_dir>` is `docs/reports` unless `.backlog-kit.toml` sets `[paths] backlog_dir` (then it is the `reports` folder next to the backlog's parent). The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Input**: "review the architecture" (full review, every applicable dimension), or one dimension to dig into ("just state management", "how is configuration handled", "look deeper at layering"). A deep-dive goes deeper on that dimension than a full review gives each one. A request to work through an earlier report goes to **Follow-up workflow**.

## The dimensions

Skip a dimension that does not apply to this project (say so in the report); never invent findings to fill one. Do not add dimensions of your own.

1. **Module and folder organisation, naming** — how the code is split (packages, folders, projects), whether each module is cohesive and self-contained, whether a "shared" or "common" area holds genuinely multi-consumer code or single-consumer code parked there out of habit, whether names match what the code does.
2. **Layering and dependency direction** — identify the layering the project claims (from its docs and folder structure; if it claims none, say so and infer the de-facto one from imports) and test the code against that claim. Grep the imports in each direction: do lower layers import higher ones, does a presentation or entry-point module reach straight into storage or an external client, does domain logic live in adapters, controllers or mappers? Look for dependency cycles between modules. Each hit is a finding unless the project documents it as an exception.
3. **Data-access patterns** — how the code reaches its data stores and external services: through one gateway layer or scattered calls; where queries and transactions are placed; whether authorization and validation are enforced where the project says they are (read the actual enforcement point, not the docs); how large result sets are handled (pagination, unbounded reads); how several stores are kept consistent when a write spans them; a multi-step write with no transaction or compensation, so a failure half-way leaves partial data; asynchronous work whose failure nobody observes or retries; caching (what is cached, where, with what invalidation, and whether stale data could be served as current); a query issued once per element of a collection (N+1); whether schema or data changes follow one disciplined, versioned path rather than ad hoc edits; and whether request and response shapes stay consistent across similar endpoints or calls.
4. **State and coordination mechanisms** — shared state, caches, event buses, message topics, observers, global singletons, browser or local storage (audit what is kept there: how sensitive it is, how keys are named and versioned, whether it is cleared on sign-out), anything one part of the system uses to tell another part something changed. **Always run the "hunt the broadcast" technique**: for every shared service, store, signal, subject, topic or listener that exposes state or an event-like operation (`refresh`, `notify`, `publish`, a version or tick counter), grep every producer and every consumer across the whole tree, then check whether they span two or more features with no direct relationship to each other and whether the unit's own name describes that role. If not, that is the finding the skill exists to catch: report the producer and consumer counts and the list of features spanned. Also flag a channel that is produced but never consumed (or the reverse), a caller that depends on a side effect in a listener it never invokes, and consumers that must run in a particular order. Do not recommend a heavyweight state-management library by default; the usual fix is a small, honestly named split.
5. **Error handling and logging** — whether there is a central error pipeline and whether features use it or have grown their own ad hoc handling; swallowed errors; whether failures in the flows that matter are observable (logs, metrics, health checks).
6. **Configuration** — typed or centralised configuration versus values scattered through the code; environment and profile handling; whether secrets are kept out of the repository (check, do not assume); whether defaults are safe.
7. **Testing seams** — a light pointer only. Are boundaries mockable (ports, interfaces, injected clients) or hard-wired; is the test shape (unit versus integration balance) sensible; is a whole module without any tests. Do not run a coverage sweep or duplicate a testing skill; mention a structural gap only when it surfaces while reviewing another dimension.
8. **Routing and entry points** (where applicable) — routes, controllers, command handlers, job schedulers, message consumers: whether they are thin, whether access control sits in a consistent place rather than inside each handler, whether loading is lazy where the platform supports it, whether lifecycles (streams, subscriptions, emitters) are cleaned up.

## Steps

### 1. Determine scope

- **Full review**: every applicable dimension, one pass each, at survey depth (2-4 concrete evidence points per dimension). When every channel turns out healthy, one line of counts per channel is enough. The one exception to the depth limit is the "hunt the broadcast" technique in the state dimension: it always lists every producer and consumer, whatever the depth.
- **Deep-dive**: the user names one dimension, or one flagged in an earlier report. Read the relevant source in full, grep every call site, quantify (counts, which features), and cite `file:line` for every claim.

### 2. Read the documented baseline, then the code

Read the project's own architecture documentation (README, `CLAUDE.md`, docs folder, decision records) for what it *claims*. That is a hypothesis, not an answer; documentation drifts. Then grep and read the real source for each in-scope dimension. Where docs and code disagree, record the mismatch as its own finding (a documentation-accuracy problem), never conflated with a code-shape finding.

Keep a private list while you work: every file you actually opened and every grep you ran. A claim must trace to something on that list. Do not infer behaviour from a folder name, a file name or a class name alone; open the file. If you could not verify something, write it as "not verified" with the reason, or leave it out.

### 3. The project's review checks

Read the project's `docs/architecture.md` and, in it, the section named `Architecture review checks` (the kit convention: that doc holds the system and component diagrams, the data-model diagram, the project structure, and what this review checks beyond its own dimensions). The section holds what this skill cannot know: the project's own stack-specific dimensions and checks (which layering it claims, which searches to run), the files that look cross-cutting, and the real case that motivated the review. Add them to the dimensions above; they never remove one.

Number the skill's dimensions D1–D8 and the project's own P1, P2, … in the order the section lists them, and give every finding the tag of the dimension it belongs to; where a project dimension overlaps one of the skill's, do one combined pass and tag it with both, written `[D4/P5]` (a finding about documentation that disagrees with the code carries the extra tag `[docs]`, for example `[D4/P5][docs]`), and state the mapping in the report header. The section says which skill dimension each of its items extends (`P1→D1, …`, or `extends D3`); an item with no such statement is assigned to the closest dimension by you, and the report header says which.

If `docs/architecture.md` has no such section, work from the dimensions above plus what the project's `CLAUDE.md` and docs say, and state in the report that no project checks were found; in a full review, offer to write the section after the user has seen the findings.

### 4. Classify each finding

- **✅ Strength** — a pattern worth keeping, cited with the same rigour as a risk (file and pattern named) so nobody "fixes" a deliberate choice. Plain bullet, no checkbox. A documented exception to a rule (the project says in its docs or code comments that it is deliberate) is listed here, with the documentation cited, not as a finding — including a sole-consumer or similar exception the documentation itself names as deliberate; an exception the docs only mention without calling it deliberate stays a finding. A write that converges when retried (client-generated ids, diff-based updates) counts as having a compensation only when the docs or a code comment say so; otherwise note the mitigation inside the finding and keep its severity. So is a healthy channel or pattern worth keeping, and a unit produced but never consumed (or the reverse) that the docs say is kept on purpose; several healthy channels may share one bullet with their counts.
- `🟡` **Worth addressing** — a real deviation, growing coupling or friction that is not urgent or user-blocking.
- `🔴` **Structural risk** — actively causing, or clearly about to cause, a correctness or maintainability problem (a layer bypassed, a dependency cycle, a write path with no transaction boundary, a stream never closed).

Every `🔴`/`🟡` finding needs concrete evidence: `file:line`, and for a coupling or coordination finding the producer and consumer counts and the features spanned. A bare "this could be an issue" with no grep behind it does not belong in the report.

### 5. Write the report, then the summary

Write `<reports_dir>/architecture-review/ARCHITECTURE_REVIEW_<YYYY-MM-DD>.md` (for a deep-dive, `ARCHITECTURE_REVIEW_<YYYY-MM-DD>_<dimension-slug>.md`). If that name exists, add `-2`, `-3`, ... on the same day; never overwrite or delete an earlier report. Create the folder if needed. Contents:

- A header: date, scope (full or which dimension), the commit or working-tree state it was run against (read from files already present, without running git — for example the branch from `.git/HEAD`; if unknown, say so), and which dimensions were skipped as not applicable.
- `### Strengths` (plain bullets), `### Findings` grouped by severity. Each finding gets a stable ID, numbered continuously across the groups in the order they are listed (`F1`, `F2`, ...) so a later deep-dive or follow-up can cite it, and is a checkbox line:
  `- [ ] F1 🔴|🟡 [D4] <file>:<line or path> — <what is wrong, evidence with counts, why it matters>`
  Omit an empty group.
- Evidence is cited inline, searches included: `grep "<pattern>" <path> → N hits`.
- `### Coverage` — a checklist, one line per dimension (the skill's and the project's) and per rule the project's section lists (a rule is one bullet, or one technique described in a paragraph), each marked `[x] covered`, `[~] partial` or `[ ] not covered`, with the number of files opened and searches run. Build it before writing the findings (it is the private list of step 2 made visible) and fill it as you go. A dimension marked `partial` or `not covered` also appears in the next section, named by its tag and with what was not looked at, so two runs can be compared on what each looked at, not only on what each found.
- `### Not verified` — anything you could not confirm and why.
- `### Candidate follow-ups` — for each `🔴`/`🟡` finding, which kit skill would record it: `bug-report` for a concrete defect or tech-debt item; `feature-plan` for a structural improvement that is a piece of planned work; `decision-record` for a finding that is really an architectural choice nobody wrote down (the report lists it as a candidate; it is not a defect). Listing only.

Finish with a chat summary: counts per severity, the report path, a short verdict on overall health, and the single most important thing to address first if anything is `🔴` or `🟡` is piling up. Then offer, in one sentence, to file selected findings; do nothing further until the user answers.

## Follow-up workflow

For revisiting an **existing** report instead of running a fresh review.

1. Resolve the report: one the user names, or the most recent `ARCHITECTURE_REVIEW_*.md` under `<reports_dir>/architecture-review/` for the dimension asked about (else the latest full review). If none exists, say so and suggest a fresh review.
2. Read it in full, then ask whether the user wants to dig deeper into one still-open (`- [ ]`) finding (a deep-dive scoped to it), or to file some of its candidate follow-ups.
3. **Filing is only with the user's confirmation, one finding at a time**: show the proposed bug or feature (title, summary, the evidence from the report), and on a yes hand it to `bug-report` or `feature-plan`, which run their own confirm-before-write step. Never file a batch in one go, and never file anything the user did not select.
4. When a deep-dive finds a flagged issue unchanged, worse or resolved since the report, say so and cite the earlier report's numbers ("still 9 consumers across 3 features, unchanged since the <date> report") rather than re-deriving them as if new. You may tick a finding in the earlier report only if the user asks.

## Guardrails

- **Never run `git commit`, `git push` or open a PR.** No git command is needed for this skill at all.
- Read-only on source. The only file this skill writes is the dated report (a tick in an earlier report only when the user asks). It never refactors, moves files or edits code, tests, config or the backlog.
- Never file a finding automatically. Listing candidates is part of the report; invoking `bug-report`, `feature-plan` or `decision-record` happens only after the user selects a finding and confirms.
- Ground every claim in files you opened or searches you ran, and cite them. Never assess from documentation prose, folder names or file names alone; a claim without evidence is dropped or marked "not verified".
- Stay whole-system and qualitative. Do not review a specific diff (that is the per-change review), and do not produce a numeric metrics sweep or a trend table across reports.
- Do not install anything or run the project's build or tests to produce findings; reading and searching are enough.
