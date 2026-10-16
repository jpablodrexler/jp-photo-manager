---
name: security-reviewer
description: >
  Security review skill. TRIGGER when code touches authentication,
  authorization or access-control rules, user input handling, data
  persistence, or dependency changes — including when implementing planned
  tasks that affect any of these areas. Do not wait to be asked: run this
  review proactively after writing security-sensitive code. Covers
  dependency vulnerabilities, the project's authorization boundary,
  sensitive data and secret handling, session/token storage, and injection
  and client-side scripting risks, using the checklists the project keeps
  in the `Security review checks` section of its `docs/security.md`. A full-codebase sweep produces one
  report per architecture layer instead of a single consolidated report —
  see "Full-Codebase Sweeps" below. Also TRIGGERS when asked to audit the
  codebase's security, and when asked to fix, address, resolve, or work
  through findings from an existing dated SECURITY_REVIEW_FINDINGS report —
  see "Fix Workflow" below.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Security Reviewer Skill

Review code in this project for security vulnerabilities, sensitive data
mishandling, and insecure patterns. Where authorization is enforced, which
keys or values are meant to be public, and how sessions are stored differ
from one project to the next: the project's own security checklist files
(see step 4 of the Workflow) describe that model, and a reviewer must read
them before flagging anything that looks like a leaked credential or a
missing check.

`<reports>` below is `docs/reports` unless `.backlog-kit.toml` sets
`[paths] backlog_dir`, in which case it is the `reports` folder inside that
directory's parent folder. The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Never run `git commit`, `git push` or open a PR.**

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing new/changed code.
- **Fix** (§9) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review code.

## Workflow

1. Identify the **scope**: a full-codebase sweep of the whole project, or a
   scoped review (single file, a list of changed files, PR, or feature).
   - **Scoped review** → follow steps 2–6 below as a single pass, producing
     one report.
   - **Full-codebase sweep** → follow "Full-Codebase Sweeps: Review by
     Layer" instead. It runs steps 2–6 once per layer, each producing its
     own report.
2. Scan for dependency changes (the project's dependency manifests and
   lockfiles).
3. Read every changed file that touches auth, access-control rules, user
   input, or data persistence before forming any opinion.
4. **Project checklists:** read the project's `docs/security.md` and, in it,
   the section named `Security review checks` (the kit convention: that doc
   holds the authentication flow, the session model and access control, and
   what this review checks in the project). Work through the subsections that
   apply to what changed, one by one; the sensitive-data and known-pitfalls
   subsections apply to every review. If the doc has no such section, review
   with the frame above plus what the project's CLAUDE.md and docs say, and
   state in the report that no project checklist was found.
5. Report findings grouped by **severity**, then by file.
6. Summarise with a short verdict and the top action items, then write the
   full report to a new, dated markdown file — see "Review Report Format &
   Output File" below. Do this on every run, not just full-codebase
   sweeps: a single-file or single-PR review still gets its own dated
   report, scoped to whatever was actually reviewed.

### Full-Codebase Sweeps: Review by Layer

When asked to review the whole project's security, don't produce one giant
consolidated report and don't review it inline in the main conversation.
Instead, split the sweep into one pass per **architecture layer**, each run
by its own subagent and producing its own dated report file (see "Process"
below).

These are the same layer boundaries `code-reviewer` uses, so a combined
code+security sweep produces parallel, directly comparable report sets per
layer.

**The layers:** the layer table (layer key, report suffix, directory scope,
primary checklist sections) comes from the
`Full-codebase sweep layers` subsection of the project's `Security review
checks`. If there is none, the sweep is not available — say so and ask for a
scope instead.

If only part of the codebase is in scope, skip the layers that don't apply.

**Process — one subagent per layer:**

Each layer's review runs in its own subagent (`Agent` tool,
`subagent_type: general-purpose`), not inline in the main conversation.

1. Before starting, check `<reports>/security-review/` for layer report
   files already dated today. If a sweep was interrupted in an earlier
   session, resume by only dispatching subagents for the layers that don't
   have a report yet for today's date.
2. The remaining applicable layers are independent of each other, so
   dispatch all of them together: one `Agent` call per layer, all issued
   in the **same message** so they run in parallel.
3. Each subagent's prompt must be self-contained — it has no memory of this
   conversation — and must include:
   - The layer key, its directory scope, and its primary checklist sections
     (copy the relevant row from the layer table into the prompt).
   - An instruction to first read this skill's `SKILL.md` for the severity
     legend and the "Review Report Format & Output File" section (§8), and
     the subsections of the project's `Security review checks` (in
     `docs/security.md`) that hold those checklist sections — don't restate the whole
     checklist in the prompt.
   - The exact output path to write:
     `<reports>/security-review/SECURITY_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
     (apply the `-2`/`-3` collision rule from §8 itself if the file already
     exists).
   - An explicit instruction to only read/review files under that layer's
     own directory scope, and to write the report file itself rather than
     just returning findings as chat text.
   - Any subagent reminders listed in the layer file (for example files that
     must never be read, or values that look like secrets but are not).
   - A short return-message instruction: report back the Critical/Warning/
     Suggestion counts and the path it wrote, not the full findings text.
4. These are launched as background agents by default — don't poll or
   sleep waiting for them; you'll be notified as each one completes.
5. Once every dispatched subagent has completed, give a short in-chat
   summary: the list of report files written, and the Critical/Warning/
   Suggestion count per layer.

Severity levels:

| Level             | Meaning                                                                                    |
| ----------------- | ------------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Exploitable vulnerability, data leak, access-control gap, or privilege escalation. Must be fixed immediately. |
| 🟡 **WARNING**    | Insecure pattern that raises risk. Should be fixed before merging.                         |
| 🟢 **SUGGESTION** | Defence-in-depth improvement or minor hardening. Fix if convenient.                        |

---

## 1. Dependency Vulnerabilities

Run the project's dependency audit tooling (its package manager's audit
command or a vulnerability scanner) for every dependency manifest in the
repository; the project's checklist files name the exact commands.

🔴 Flag any package with a `critical` or `high` severity audit finding that
has a fix available.

🟡 Flag core framework or backend-client libraries significantly behind
latest with a known security advisory.

🟢 Flag development-only dependencies with audit findings — lower risk but
still worth updating, since build tools can be exploited in CI and in the
deployment build environment.

---

## 2.–7. Project security checklists

The stack-specific checklist sections are not part of this skill; they live
in the project's `Security review checks` section of `docs/security.md`
(see Workflow step 4). Their section numbers
are kept, so findings and fixes can cite them:

- **§2 Authorization boundary** — where access control is actually enforced
  and what the client side must never be trusted to enforce: see the
  project's security checklist files.
- **§3 Sensitive data handling** — logging, error responses, keys and
  secrets (including which values are meant to be public): see the
  project's security checklist files.
- **§4 Authentication & session security** — token and session storage,
  credential hashing, auth flow correctness: see the project's security
  checklist files.
- **§5 Migration- and injection-specific checks** — schema/policy
  migrations, query and command injection, file-system security: see the
  project's security checklist files.
- **§6 Client-side security** — storage of sensitive data, scripting
  injection, cross-origin and redirect configuration: see the project's
  security checklist files.
- **§7 Known project-specific pitfalls** — the project's table of recurring
  security mistakes: see the project's security checklist files.

---

## 8. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Security Review: <file or PR title>

### 🔴 Critical
- `path/to/file:42` — <vulnerability description and exploit scenario>

### 🟡 Warnings
- `path/to/file:15` — <insecure pattern and recommended fix>

### 🟢 Suggestions
- `path/to/file:88` — <hardening improvement>

### Verdict
<One or two sentences: overall risk level, whether it is safe to merge, and
the single most important issue to fix first.>
```

If there are no findings in a severity category, omit that category
entirely.

### Write the report to a dated markdown file

Every time this skill runs — full-codebase sweep, single file, or PR
review — also write the findings to a new markdown file so work can be
resumed later without re-deriving context.

**Scoped review (single file, PR, or feature) — one file:**

- **Path:** `<reports>/security-review/SECURITY_REVIEW_FINDINGS_{YYYY-MM-DD}.md`
  (repo root, today's date, ISO 8601). If a file for that date already
  exists, append `-2`, `-3`, etc. rather than overwriting the earlier run's
  report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:**
  `<reports>/security-review/SECURITY_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
  where `{layer}` is the report suffix from the layer table. Same `-2`,
  `-3` collision rule.
- Write each layer's file as soon as that layer's pass is done — don't hold
  all layers in memory to write at once.

**Both cases:**

- This directory is gitignored — reports are local working artifacts, not
  committed history. Create the directory if it doesn't exist yet.
- **Content:** the same Critical/Warnings/Suggestions grouping as the
  in-chat summary, using GitHub task-list checkboxes (`- [ ]`) per finding
  instead of plain bullets. Include a short header noting the scope
  reviewed and which commit/state the review was run against. Only include
  categories that have findings.
- **Scope of content:** write only what was actually found in *this* run —
  don't carry forward unresolved items from a previous dated report by
  default.
- Do not overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.
- **Never quote a secret value in a report.** If a finding involves an
  actual secret or credential, reference its location and
  variable/property name only — never copy the literal value into the
  report file or the in-chat summary.

---

## 9. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new
review. It is interactive and incremental: fix a chunk, check it off, ask
what's next. **This workflow never commits** — all changes stay uncommitted
in the working tree for the user to review and commit themselves.

### 9.1 Locate the report(s) for a date

1. If the user names a specific report file, skip straight to §9.2 with
   that file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `<reports>/security-review/SECURITY_REVIEW_FINDINGS_*.md` file. If none
   exists, say so and stop.
2. List every report file for that date. For each, quickly check whether
   it has any unchecked (`- [ ]`) boxes left; drop fully-checked-off
   reports from the list.
3. **If exactly one report remains**, use it directly. **If more than one
   remains**, ask the user which report to work on first, showing the
   layer name (or scope) and a rough Critical/Warning/Suggestion count for
   each. Work on exactly one report at a time. Default to surfacing 🔴
   Critical-heavy reports first if the user has no preference.

### 9.2 Read the report, then ask what to fix

1. Read the full report before asking anything.
2. Ask the user whether they want to fix an entire severity category, or a
   specific finding — only offer categories/findings with unchecked boxes
   remaining.

### 9.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected file(s) and understand the finding in full context
   before changing anything.
2. Apply the fix. The project's checklist sections (§2–§7) tell you what
   "right" looks like for that category of issue. For an access-policy or
   schema fix specifically, follow `database-reviewer`'s rule: a new
   migration, never an edit to an already-applied one.
3. If a fix hinges on a real design decision (a breaking auth-flow change,
   several equally valid remediations), stop and ask the user before
   proceeding.
4. Verify the fix: run the narrowest relevant tests for the affected code,
   and for anything access-control-related, re-check scoping manually with
   two separate accounts.
5. Update tests for the new shape of the code.
6. Mark the finding complete in the report file immediately: change
   `- [ ]` to `- [x]` and append a bolded outcome note:
   - `**Fixed:** <what changed and why, one or two sentences>.`
   - `**Evaluated, no change made:** <rationale>`
7. If fixing one finding incidentally resolves another still-unchecked one,
   check that one off too with a note explaining it was fixed as a
   byproduct.
8. Never write an actual secret value into the fix commentary or the
   report file — reference it by name/location only (same rule as §8).

### 9.4 Continue until done

After finishing the selected scope, run the widest verification available
(the full test suite and a production build) once before asking what's
next. Then:

- If the current report still has unchecked findings, repeat from §9.2.
- If the current report is now fully checked off, go back to §9.1.

Stop when the user says to stop, or when every report for the date is
fully checked off.

### 9.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command
as part of this workflow, not even implicitly. Leave all changes
uncommitted so the user can review the diff and commit it themselves.
