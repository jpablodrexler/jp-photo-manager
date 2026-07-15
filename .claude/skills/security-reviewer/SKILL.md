---
name: security-reviewer
description: >
  Security review skill for the JPPhotoManager project (Spring Boot 3.4 /
  Java 21 backend + Angular 19 frontend). TRIGGER when code touches
  authentication, authorization, file I/O, user input handling, dependency
  changes, or data persistence — including when implementing OpenSpec tasks
  that affect any of these areas. Do not wait to be asked: run this review
  proactively after writing security-sensitive code. Covers dependency
  vulnerabilities, sensitive data handling, path traversal, injection,
  Spring Security misconfigurations, JWT/cookie security, and frontend
  storage anti-patterns. A full-codebase sweep of the whole web application
  produces one report per architecture layer instead of a single
  consolidated report — see "Full-Codebase Sweeps" below. Also TRIGGERS
  when asked to fix, address, resolve, or work through findings from an
  existing dated SECURITY_REVIEW_FINDINGS report — see "Fix Workflow" below.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Security Reviewer Skill

Review code in the JPPhotoManager project for security vulnerabilities, sensitive
data mishandling, and insecure patterns. The project has two sub-projects with
distinct stacks; apply the relevant checklist(s) based on which files changed.

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing new/changed code.
- **Fix** (§9) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review code.

## Workflow

1. Identify the **scope**: a full-codebase sweep of the whole web application,
   or a scoped review (single file, PR, feature, or one sub-project).
   - **Scoped review** → follow steps 2–7 below as a single pass, producing
     one report (unchanged from prior behavior).
   - **Full-codebase sweep** → follow "Full-Codebase Sweeps: Review by Layer"
     instead. It runs steps 2–7 once per layer, each producing its own report.
2. Identify which sub-project(s) are affected: **backend** (Java), **frontend**
   (Angular/TypeScript), or both.
3. Scan for dependency changes (`pom.xml`, `package.json`, `package-lock.json`).
4. Read every changed file that touches auth, file I/O, user input, or data
   persistence before forming any opinion.
5. Work through the relevant checklist(s) below, section by section.
6. Report findings grouped by **severity**, then by file.
7. Summarise with a short verdict and the top action items, then write the
   full report to a new, dated markdown file — see "Review Report Format &
   Output File" below. Do this on every run, not just full-codebase sweeps: a
   single-file or single-PR review still gets its own dated report, scoped to
   whatever was actually reviewed.

### Full-Codebase Sweeps: Review by Layer

When asked to review the whole web application's security (or the entire
backend, or the entire frontend), don't produce one giant consolidated
report and don't review it inline in the main conversation. Instead, split
the sweep into one pass per **architecture layer**, each run by its own
subagent and producing its own dated report file (see "Process" below). This
keeps each pass's context small (a single layer's files, isolated in its own
subagent, not the whole codebase piled into the orchestrating conversation),
makes the sweep resumable across sessions, and lets the user later fix one
layer's findings at a time (§9) instead of wading through everything at once.

These are the same layer boundaries `code-reviewer` uses, so a combined
code+security sweep of the app produces parallel, directly comparable report
sets per layer.

**The layers:**

| Layer key               | Report suffix            | Directory scope                                                                                                                                            | Primary checklist sections                                    |
| ------------------------ | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Backend domain           | `backend-domain`          | `backend/.../domain/**`                                                                                                                                   | §3.3 (low surface — pure POJOs/interfaces, few findings expected) |
| Backend application      | `backend-application`     | `backend/.../application/usecase/**`, `application/exception/**`                                                                                          | §3.1, §3.2 (custom exceptions), §3.3, §4.3                      |
| Backend api               | `backend-api`              | `backend/.../infrastructure/web/**` (controllers, DTOs, exception handlers, filters)                                                                      | §2.1 (endpoints returning file contents), §3.2, §3.3, §4.1, §6.3 (`@CrossOrigin` on controllers) |
| Backend infrastructure   | `backend-infrastructure`  | `backend/.../infrastructure/persistence/**`, `infrastructure/service/**`, `infrastructure/kafka/**`, `infrastructure/batch/**`, `infrastructure/config/**`, `infrastructure/adapter/**` | §2 (full), §3.3, §4.2, §4.3 (encoder bean), §5.1, §5.2, §6.3 (`AppConfig`) |
| Frontend core             | `frontend-core`            | `frontend/src/app/core/**`                                                                                                                                | §6.1                                                             |
| Frontend features         | `frontend-features`        | `frontend/src/app/features/**`                                                                                                                            | §6.1, §6.2                                                       |
| Frontend shared           | `frontend-shared`          | `frontend/src/app/shared/**`                                                                                                                              | §6.2                                                             |
| Cross-cutting             | `cross-cutting`            | Both sub-projects; no single directory                                                                                                                    | §1 (dependency vulnerabilities — `pom.xml`/`package.json` are project-wide, not per-layer), §7, any finding whose root cause spans two layers at once (e.g. JWT-in-URL: a backend controller emitting the URL and a frontend `EventSource` consuming it) |

If only the backend (or only the frontend) is in scope, skip the layers that
don't apply — e.g. a "review the whole backend's security" request produces
4 reports (domain, application, api, infrastructure) plus a backend-scoped
cross-cutting report, not all 8.

**Process — one subagent per layer:**

Each layer's review runs in its own subagent (`Agent` tool,
`subagent_type: general-purpose`), not inline in the main conversation. This
is what actually keeps the sweep within session limits: each subagent starts
cold, reads only its own layer's files, writes its own report, and never
touches the orchestrating conversation's context.

1. Before starting, check `docs/security-review/` for layer report files
   already dated today. If a sweep was interrupted in an earlier session,
   resume by only dispatching subagents for the layers that don't have a
   report yet for today's date — don't redo layers already completed.
2. The remaining applicable layers are independent of each other (no layer's
   review depends on another's findings), so dispatch all of them together:
   one `Agent` call per layer, all issued in the **same message** so they run
   in parallel. Don't dispatch them one at a time and wait in between.
3. Each subagent's prompt must be self-contained — it has no memory of this
   conversation — and must include:
   - The layer key, its directory scope, and its primary checklist sections
     (copy the relevant row from the layer table above into the prompt).
   - An instruction to first read
     `.claude/skills/security-reviewer/SKILL.md` in the repo for the full
     text of those checklist sections, the severity legend, and the "Review
     Report Format & Output File" section (§8) — don't restate the whole
     checklist in the prompt, point the subagent at the file.
   - The exact output path to write:
     `docs/security-review/SECURITY_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
     (apply the `-2`/`-3` collision rule from §8 itself if the file already
     exists).
   - An explicit instruction to only read/review files under that layer's own
     directory scope — not the whole repo — and to write the report file
     itself rather than just returning findings as chat text.
   - **A reminder never to read `JPPhotoManagerWeb/k8s/secret.yaml` or
     `JPPhotoManagerWeb/k8s/catalog-volumes.yaml`** (relevant to the
     `backend-infrastructure` and `cross-cutting` subagents in particular,
     since they're the ones most likely to touch config/deployment files) —
     see §3.3.
   - A short return-message instruction: report back the Critical/Warning/
     Suggestion counts and the path it wrote, not the full findings text —
     the orchestrating conversation only needs the summary, the report file
     is the full record.
4. These are launched as background agents by default — don't poll or sleep
   waiting for them; you'll be notified as each one completes.
5. Once every dispatched subagent has completed, give a short in-chat
   summary: the list of report files written, and the Critical/Warning/
   Suggestion count per layer (from each subagent's return message), so the
   user can see at a glance which layer needs attention first.

Severity levels:

| Level             | Meaning                                                                                    |
| ----------------- | ------------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Exploitable vulnerability, data leak, or privilege escalation. Must be fixed immediately.  |
| 🟡 **WARNING**    | Insecure pattern that raises risk. Should be fixed before merging.                         |
| 🟢 **SUGGESTION** | Defence-in-depth improvement or minor hardening. Fix if convenient.                        |

---

## 1. Dependency Vulnerabilities

### 1.1 Backend (Maven)

Run the OWASP dependency check and review the output:

```bash
cd JPPhotoManagerWeb/backend
mvn org.owasp:dependency-check-maven:check
```

Alternatively, audit with the Maven Versions plugin:

```bash
mvn versions:display-dependency-updates
```

🔴 Flag any dependency with a known CVE rated **CRITICAL** or **HIGH** in the
NVD database.

🟡 Flag any Spring Boot, Spring Security, or Flyway version that is more than
two minor versions behind the latest stable release — these libraries receive
frequent security patches.

🟡 Flag any transitive dependency that is being overridden with an older version
via `<dependencyManagement>` — verify the override is intentional.

### 1.2 Frontend (npm)

```bash
cd JPPhotoManagerWeb/frontend
npm audit --audit-level=high
```

🔴 Flag any package with a `critical` or `high` severity audit finding that has
a fix available (`npm audit fix`).

🟡 Flag packages that are significantly outdated and have known security advisories
(check https://security.snyk.io or the npm advisory database).

🟢 Flag `devDependencies` with audit findings — lower risk but still worth
updating, since build tools can be exploited in CI.

---

## 2. File System Security (Critical for a Photo Manager)

The application reads, writes, and thumbnails arbitrary file paths supplied by
the user. This is the highest-risk attack surface in this codebase.

### 2.1 Path Traversal

🔴 Flag any file operation that uses a user-supplied path directly without
canonicalisation and containment checks. The canonical fix is:

```java
Path base = Paths.get(allowedRootDirectory).toRealPath();
Path target = base.resolve(userSuppliedRelativePath).normalize();
if (!target.startsWith(base)) {
    throw new SecurityException("Path traversal attempt: " + target);
}
```

🔴 Flag any endpoint that returns file contents (bytes, streams) when the path is
derived from a request parameter without the check above.

🔴 Flag any thumbnail URL or file URL that embeds an absolute filesystem path
exposed in an API response — callers should receive an opaque ID, not a raw path.

### 2.2 File Type Validation

🔴 Flag any file-upload or file-read path that relies **only** on the file
extension to determine content type. Magic-byte validation must also be done:

```java
// Apache Commons Imaging already does this for supported formats
// For a generic check, use Tika or Files.probeContentType()
String detectedType = tika.detect(inputStream);
if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
    throw new IllegalArgumentException("Unsupported file type: " + detectedType);
}
```

🟡 Flag any code that writes a user-supplied filename directly to disk without
sanitising it (remove `..`, `/`, `\`, null bytes, and leading dots).

### 2.3 Thumbnail Generation

🟡 Flag thumbnail generation code that does not set limits on image dimensions
before decoding. Decompression bomb (e.g., a 1×1 JPEG that expands to 100 MB)
can exhaust heap. Use a library that supports dimension-before-decode checks.

🟢 Flag any temporary file created during thumbnail generation that is not
cleaned up in a `finally` block or `try-with-resources`.

---

## 3. Sensitive Data Handling

### 3.1 Logging

🔴 Flag any `log.info`, `log.debug`, or `log.error` call that includes:
- Passwords or password hashes
- JWT token values
- Full absolute file paths that contain the user's home directory (e.g.,
  `/home/alice/Pictures/private/...`) — log only relative paths or asset IDs.

🟡 Flag any exception that is caught and re-thrown with the original message
embedded in an API response — stack traces and internal paths must not reach
the client.

### 3.2 API Error Responses

🔴 Flag any `@ExceptionHandler` or catch block that returns a stack trace,
Hibernate error message, or SQL snippet in the response body. Return a safe,
generic message instead:

```java
// Wrong
return ResponseEntity.status(500).body(e.getMessage()); // may expose schema

// Right
log.error("Unexpected error", e);
return ResponseEntity.status(500).body("An internal error occurred.");
```

🟡 Flag 404 responses that confirm the existence of resources the caller
should not know about (e.g., `"Asset 42 exists but you cannot see it"` vs
a plain 404).

### 3.3 Hardcoded Secrets

🔴 Flag any hardcoded password, API key, secret key, or token in Java source,
`application.yml`, `application.properties`, or Angular environment files.
Secrets must come from environment variables or a secrets manager:

```yaml
# Wrong
spring.datasource.password: mysecretpassword

# Right
spring.datasource.password: ${POSTGRES_PASSWORD:postgres}
```

🔴 Flag any secret committed to `application-local.yml` that is **not** in
`.gitignore`.

🚫 **Never read `JPPhotoManagerWeb/k8s/secret.yaml` or
`JPPhotoManagerWeb/k8s/catalog-volumes.yaml`** while hunting for hardcoded
secrets — per `JPPhotoManagerWeb/CLAUDE.md`, both hold real secrets and
machine-specific paths and must never be read under any circumstances, not
even as part of a broad "review all the code" sweep. Both are gitignored by
design (that's the correct pattern, not a finding) and have `.example`
counterparts safe to read instead if you need to reason about their
structure. If a full-codebase sweep would otherwise walk `k8s/`, skip these
two files specifically rather than skipping the whole directory.

---

## 4. Authentication & JWT / Cookie Security

### 4.1 HttpOnly Cookie (not Authorization header)

🔴 Flag any code that reads the JWT from a query parameter
(`?token=...`) or a custom header for `EventSource` or `<img>` endpoints.
Tokens in URLs appear in server logs and browser history. The project uses
HttpOnly cookies — see `java-developer` skill §15.1 for the correct pattern.

🔴 Flag any `ResponseCookie` that is missing `.httpOnly(true)`, `.sameSite("Strict")`,
or `.path("/")`.

🟡 Flag any `ResponseCookie` that sets `secure: false` when the app is deployed
over HTTPS.

### 4.2 Spring Security Filter Chain

🔴 Flag any `SecurityFilterChain` that does not include
`.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as the **first**
rule. Missing this causes `AuthorizationDeniedException` on `SseEmitter` async
dispatch threads (see `java-developer` skill §15.2).

🔴 Flag any endpoint that handles write operations (`POST`, `PUT`, `PATCH`,
`DELETE`) but is permitted without authentication.

🟡 Flag `http.csrf().disable()` without a comment explaining why — CSRF
protection should only be disabled when the API is stateless (JWT-based) and
does not use cookie-based session auth. If the project switches to HttpOnly
cookies, CSRF protection via `SameSite=Strict` must be verified.

🟡 Flag Spring Boot Actuator endpoints (`/actuator/**`) that are exposed
without authentication in production profiles.

### 4.3 Password Storage

🔴 Flag any code that stores or compares passwords in plaintext.

🔴 Flag any use of MD5 or SHA-1 for password hashing. Use BCrypt with a work
factor ≥ 10:

```java
new BCryptPasswordEncoder(12)
```

🟡 Flag any `UserDetailsService` that loads users from a hardcoded in-memory
list in a non-test profile — production must load from the repository.

---

## 5. Injection Vulnerabilities

### 5.1 SQL / JPQL Injection

🔴 Flag any `@Query` that concatenates user input into the JPQL or SQL string
instead of using named parameters:

```java
// Wrong
@Query("SELECT a FROM Asset a WHERE a.fileName = '" + fileName + "'")

// Right
@Query("SELECT a FROM Asset a WHERE a.fileName = :fileName")
List<Asset> findByFileName(@Param("fileName") String fileName);
```

🟡 Flag any `EntityManager.createNativeQuery()` call that uses string
concatenation with user input.

### 5.2 OS Command Injection

🔴 Flag any call to `Runtime.exec()`, `ProcessBuilder`, or similar that
includes a user-supplied value without shell-escape:

```java
// Wrong — user supplies folderPath
Runtime.getRuntime().exec("ls " + folderPath);

// Right — pass as separate argument, never via shell string
new ProcessBuilder("ls", folderPath).start();
```

🔴 Flag any image-processing library invocation (e.g., ImageMagick via CLI
wrapper) that interpolates user-supplied filenames into a shell command.

---

## 6. Frontend Security

### 6.1 Token & Sensitive Data in Storage

🔴 Flag any Angular code that stores the JWT token string in `localStorage`,
`sessionStorage`, or a cookie accessible to JavaScript. The token must live
only in an HttpOnly cookie managed by the backend. What may be stored in
`localStorage` is non-sensitive session metadata (username, expiry timestamp).

🔴 Flag any `localStorage.setItem` call whose value is derived from an API
response field named `token`, `accessToken`, `jwt`, or similar.

### 6.2 XSS

🔴 Flag any use of `[innerHTML]` binding with user-supplied content that is not
explicitly sanitised through Angular's `DomSanitizer`.

🟡 Flag any Angular template that renders a user-controlled URL in an `href` or
`src` attribute without `sanitizer.bypassSecurityTrustUrl()` being justified
by a comment.

🟡 Flag any `bypassSecurityTrust*` call — each one requires a clear justification
comment explaining why sanitisation is safe to bypass.

### 6.3 CORS & API Configuration

🟡 Flag `@CrossOrigin(origins = "*")` on controllers — wildcard origins are
acceptable in development but must be restricted to known frontend origins in
production via `AppConfig.corsFilter()`.

🟡 Flag the CORS allowed-methods list in `AppConfig` if it is missing any HTTP
method actually used by the API (e.g., `PATCH` missing when `@PatchMapping`
endpoints exist).

---

## 7. Known Project-Specific Security Pitfalls

| Pitfall | What to look for |
| ------- | ---------------- |
| Path traversal in `StorageServiceAdapter` | `folderPath` or `fileName` used in `Paths.get(...)` without containment check |
| Thumbnail URL leaks absolute path | API response includes raw filesystem path instead of opaque `/api/assets/{id}/thumbnail` |
| JWT in query param for SSE | `EventSource` URL contains `?token=...` — use HttpOnly cookie instead |
| BCrypt work factor too low | `new BCryptPasswordEncoder()` with default or factor < 10 |
| Actuator endpoints open | `/actuator/health`, `/actuator/env` accessible without auth in prod |
| Hardcoded DB password | `application.yml` with literal `postgres` password not overridden by env var |
| CSRF disabled without SameSite | `csrf().disable()` when using HttpOnly cookies — verify `SameSite=Strict` is set |
| Stack trace in 500 response | `e.getMessage()` or `e.toString()` returned directly in response body |
| Decompression bomb | Image decoded without dimension check before allocating pixel buffer |
| User-supplied filename on disk | `fileName` used directly in `Files.write(Paths.get(dir, fileName))` without sanitisation |

---

## 8. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Security Review: <file or PR title>

### 🔴 Critical
- `path/to/File.java:42` — <vulnerability description and exploit scenario>

### 🟡 Warnings
- `path/to/file.ts:15` — <insecure pattern and recommended fix>

### 🟢 Suggestions
- `path/to/File.java:88` — <hardening improvement>

### Verdict
<One or two sentences: overall risk level, whether it is safe to merge, and
the single most important issue to fix first.>
```

If there are no findings in a severity category, omit that category entirely.

### Write the report to a dated markdown file

Every time this skill runs — full-codebase sweep, single file, or PR review —
also write the findings to a new markdown file so work can be resumed later
without re-deriving context.

**Scoped review (single file, PR, feature, or one sub-project) — one file:**

- **Path:** `docs/security-review/SECURITY_REVIEW_FINDINGS_{YYYY-MM-DD}.md`
  (repo root, today's date, ISO 8601). If a file for that date already exists
  (e.g., a second review the same day), append `-2`, `-3`, etc. before `.md`
  rather than overwriting the earlier run's report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:**
  `docs/security-review/SECURITY_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
  where `{layer}` is the report suffix from the layer table (e.g.
  `backend-infrastructure`, `frontend-core`, `cross-cutting`). Same `-2`,
  `-3` collision rule, applied per date+layer combination.
- Write each layer's file as soon as that layer's pass is done (see the
  "Process" steps above) — don't hold all layers in memory to write at once.

**Both cases:**

- This directory is gitignored — reports are local working artifacts, not
  committed history. Create the directory if it doesn't exist yet.
- **Content:** the same Critical/Warnings/Suggestions grouping as the in-chat
  summary, using GitHub task-list checkboxes (`- [ ]`) per finding instead of
  plain bullets, so items can be checked off as they're fixed. Include a short
  header noting the scope reviewed (full codebase sweep + layer name, vs. a
  specific file/PR) and which commit(s)/state the review was run against.
  Only include categories that have findings — omit empty ones.
- **Scope of content:** write only what was actually found in *this* run —
  don't carry forward unresolved items from a previous dated report by
  default. If asked to produce a combined or updated backlog, do that
  explicitly as its own step rather than silently merging.
- Do not overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.
- **Never quote secret values in a report.** If a finding involves an actual
  hardcoded secret (§3.3), reference its location and variable/property name
  only — never copy the literal secret value into the report file or the
  in-chat summary.

---

## 9. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new review.
It is interactive and incremental: fix a chunk, check it off, ask what's next.
**This workflow never commits** — all changes stay uncommitted in the working
tree for the user to review and commit themselves.

### 9.1 Locate the report(s) for a date

1. If the user names a specific report file, skip straight to §9.2 with that
   file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `docs/security-review/SECURITY_REVIEW_FINDINGS_*.md` file. If none exists,
   say so and stop — there is nothing to fix.
2. List every report file for that date (there may be several `-2`/`-3`
   reruns per layer — treat each filename, suffix included, as a distinct
   report). For each, quickly check whether it has any unchecked (`- [ ]`)
   boxes left; drop fully-checked-off reports from the list.
3. **If exactly one report remains**, use it directly — don't make the user
   pick from a list of one. **If more than one remains** (the normal case
   after a layer-split full-codebase sweep, or several same-day scoped
   reviews), ask the user which report to work on first, showing the layer
   name (or scope, for a pre-layer-split report) and a rough Critical/Warning/
   Suggestion count for each so they can prioritize. Work on exactly one
   report at a time — don't mix findings from two reports into a single fix
   pass. Given the severity legend (§ above), default to surfacing 🔴
   Critical-heavy reports first if the user has no preference.

### 9.2 Read the report, then ask what to fix

1. Read the full report before asking anything, so the menu of categories/
   findings you present next is accurate and reflects what's already checked
   off from prior sessions.
2. Do not silently pick a starting point or scope. Ask the user whether they
   want to fix:
   - An entire severity category (🔴 Critical, 🟡 Warning, or 🟢 Suggestion), or
   - A specific finding — let them name it, or list the still-unchecked
     findings in a category for them to choose from.

   Only offer categories/findings that still have unchecked (`- [ ]`) boxes; a
   category with everything already checked off isn't worth presenting again.

### 9.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected file(s) and understand the finding in the full context
   of the surrounding code before changing anything — the report is a
   pointer, not a substitute for reading the code.
2. Apply the fix. The report tells you what's wrong; the checklist sections
   above (1–7) tell you what "right" looks like for that category of issue.
3. If a fix hinges on a real design decision rather than just applying a
   known pattern — e.g. a breaking API/contract change, a migration to a
   different auth mechanism, or several equally valid remediations — stop and
   ask the user before proceeding instead of picking one unilaterally.
4. Verify the fix before moving on: compile/build, then run the narrowest
   relevant test scope (single test class/spec). For Java, prefer a **clean**
   compile/test (`mvn clean test-compile` or `mvn clean test`) after changing
   any method or type signature — incremental builds can silently skip
   recompiling dependent test files and report a false green.
5. Update tests for the new shape of the code (renamed types/methods, changed
   signatures, moved files) — don't leave stale references or stale test
   names behind.
6. Mark the finding complete in the report file immediately, not batched at
   the end: change `- [ ]` to `- [x]` and append a bolded outcome note to the
   same line, in the same style as prior fix sessions:
   - `**Fixed:** <what changed and why, one or two sentences>.`
   - `**Evaluated, no change made:** <rationale>` — for findings where, after
     investigation, the right call is to leave the code as-is (e.g. a
     `🟢 Suggestion` hardening idea judged not worth the added complexity).
     Document why so the item isn't silently dropped.
   Updating the report per-finding (not at the end of the whole scope) means
   an interrupted session still leaves an accurate resume point.
7. If fixing one finding incidentally resolves another still-unchecked one
   (e.g. moving JWT reading from a query param to the cookie also fixes a
   related logging Warning on the same class), check that one off too with a
   note explaining it was fixed as a byproduct — don't leave it unchecked
   just because it wasn't the primary target.
8. Never write an actual secret value into the fix commentary or the report
   file, even when describing what was rotated or removed — reference it by
   name/location only (same rule as §8).

### 9.4 Continue until done

After finishing the selected scope, run the widest verification available
(full backend suite, full frontend Cypress suite, lint, production build)
once before asking what's next — don't let per-finding narrow tests substitute
for a full-suite check when a scope is done. Then:

- If the current report still has unchecked findings, repeat from §9.2: ask
  what to fix next **within the same report**, offering only what's still
  unchecked.
- If the current report is now fully checked off, go back to §9.1 — re-list
  the remaining reports for the date (if this was a layer-split sweep, there
  are likely others) and ask the user whether to move to another one or stop.

Stop when the user says to stop, or when every report for the date is fully
checked off.

### 9.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command as
part of this workflow, not even implicitly. Leave all changes uncommitted so
the user can review the diff and commit it themselves.
