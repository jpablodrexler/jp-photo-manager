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
  storage anti-patterns.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Security Reviewer Skill

Review code in the JPPhotoManager project for security vulnerabilities, sensitive
data mishandling, and insecure patterns. The project has two sub-projects with
distinct stacks; apply the relevant checklist(s) based on which files changed.

## Workflow

1. Identify which sub-project(s) are affected: **backend** (Java), **frontend**
   (Angular/TypeScript), or both.
2. Scan for dependency changes (`pom.xml`, `package.json`, `package-lock.json`).
3. Read every changed file that touches auth, file I/O, user input, or data
   persistence before forming any opinion.
4. Work through the relevant checklist(s) below, section by section.
5. Report findings grouped by **severity**, then by file.
6. Summarise with a short verdict and the top action items.

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

---

## 4. Authentication & JWT / Cookie Security

### 4.1 HttpOnly Cookie (not Authorization header)

🔴 Flag any code that reads the JWT from a query parameter
(`?token=...`) or a custom header for `EventSource` or `<img>` endpoints.
Tokens in URLs appear in server logs and browser history. The project uses
HttpOnly cookies — see `java-developer` skill §14.1 for the correct pattern.

🔴 Flag any `ResponseCookie` that is missing `.httpOnly(true)`, `.sameSite("Strict")`,
or `.path("/")`.

🟡 Flag any `ResponseCookie` that sets `secure: false` when the app is deployed
over HTTPS.

### 4.2 Spring Security Filter Chain

🔴 Flag any `SecurityFilterChain` that does not include
`.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as the **first**
rule. Missing this causes `AuthorizationDeniedException` on `SseEmitter` async
dispatch threads (see `java-developer` skill §14.2).

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
| Path traversal in `StorageService` | `folderPath` or `fileName` used in `Paths.get(...)` without containment check |
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

## 8. Review Report Format

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
