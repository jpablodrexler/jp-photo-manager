---
name: code-reviewer
description: >
  Code review skill for the JPPhotoManager project (Spring Boot 3.4 / Java 21
  backend + Angular 19 frontend). TRIGGER after implementing any feature, fix,
  or refactor — including after completing an OpenSpec task or a set of tasks.
  Do not wait to be asked: review code proactively after writing it. Also
  triggers when explicitly asked to review a pull request, file, or change.
  Covers both sub-projects (backend and frontend) and all cross-cutting
  concerns: hexagonal architecture layering, naming, transactions, async,
  testing, and TypeScript/Java style rules. A full-codebase sweep of the whole
  web application produces one report per architecture layer instead of a
  single consolidated report — see "Full-Codebase Sweeps" below. Also TRIGGERS
  when asked to fix, address, resolve, or work through findings from an
  existing dated CODE_REVIEW_FINDINGS report — see "Fix Workflow" below.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Code Reviewer Skill

Review code in the JPPhotoManager project against its documented architecture,
conventions, and known pitfalls. The project has two sub-projects with distinct
stacks; apply the relevant checklist(s) based on which files are under review.

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing new/changed code.
- **Fix** (§17) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review code.

## Workflow

1. Identify the **scope**: a full-codebase sweep of the whole web application,
   or a scoped review (single file, PR, feature, or one sub-project).
   - **Scoped review** → follow steps 2–6 below as a single pass, producing
     one report (unchanged from prior behavior).
   - **Full-codebase sweep** → follow "Full-Codebase Sweeps: Review by Layer"
     instead. It runs steps 2–6 once per layer, each producing its own report.
2. Identify which sub-project(s) are affected: **backend** (Java), **frontend**
   (Angular/TypeScript), or both.
3. Read every changed file before forming any opinion.
4. Work through the relevant checklist(s) below, section by section.
5. Report findings grouped by **severity**, then by file.
6. Summarise with a short verdict and the top action items.
7. Write the full report to a new, dated markdown file — see
   "Review Report Format & Output File" below. Do this on every run, not just
   full-codebase sweeps: a single-file or single-PR review still gets its own
   dated report, scoped to whatever was actually reviewed.

### Full-Codebase Sweeps: Review by Layer

When asked to review all the code in the web application (or the entire
backend, or the entire frontend), don't produce one giant consolidated
report and don't review it inline in the main conversation. Instead, split
the sweep into one pass per **architecture layer**, each run by its own
subagent and producing its own dated report file (see "Process" below). This
keeps each pass's context small (a single layer's files, isolated in its own
subagent, not the whole codebase piled into the orchestrating conversation),
makes the sweep resumable across sessions, and lets the user later fix one
layer's findings at a time (§17) instead of wading through everything at
once.

**The layers:**

| Layer key                | Report suffix          | Directory scope                                                                                                    | Primary checklist sections                                    |
| ------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Backend domain           | `backend-domain`         | `backend/.../domain/**`                                                                                                                   | §1.1, §5, §8 (domain-model rows)                                 |
| Backend application      | `backend-application`    | `backend/.../application/usecase/**`                                                                                                      | §1.1, §1.2, §2, §4, §7, §8 (use-case rows)                       |
| Backend api               | `backend-api`             | `backend/.../infrastructure/web/**` (controllers, request/response DTOs)                                                                  | §1.1 (controller delegation), §3.4, §6 (DTO/entity drift), §8 (DTO rows) |
| Backend infrastructure   | `backend-infrastructure` | `backend/.../infrastructure/persistence/**`, `infrastructure/service/**`, `infrastructure/kafka/**`, `infrastructure/batch/**`, `infrastructure/config/**` | §1.2, §3.1–3.3, §5, §6, §7                                       |
| Frontend core             | `frontend-core`           | `frontend/src/app/core/**`                                                                                                                | §10.2, §11, §12                                                  |
| Frontend features         | `frontend-features`       | `frontend/src/app/features/**`                                                                                                            | §10.1, §10.3, §11, §12                                           |
| Frontend shared           | `frontend-shared`         | `frontend/src/app/shared/**`                                                                                                              | §1.3, §10.1, §11, §12                                            |
| Cross-cutting             | `cross-cutting`           | Both sub-projects; no single directory                                                                                                    | §9, §13, §14, delegate-only port/adapter or service pairs (§1.2/§10.2), dependency-direction violations, systemic naming patterns |

If only the backend (or only the frontend) is in scope, skip the layers that
don't apply — e.g. a "review the whole backend" request produces 4 reports
(domain, application, api, infrastructure) plus a backend-scoped
cross-cutting report, not all 8.

**What goes in the cross-cutting report:** findings that don't belong to one
file's home layer — backend testing conventions (§9) and Cypress conventions
(§13), comment/code-style patterns (§14) that recur across many files, and
anything whose root cause spans two layers at once (e.g. a delegate-only
port/adapter pair, where the finding is really about the port *and* the
adapter *and* the callers, not just one of them). A naming or architecture
violation that's local to a single file still goes in that file's own layer
report, even if the rule itself is defined in a "cross-cutting" section like
§8/§12.

**Process — one subagent per layer:**

Each layer's review runs in its own subagent (`Agent` tool,
`subagent_type: general-purpose`), not inline in the main conversation. This
is what actually keeps the sweep within session limits: each subagent starts
cold, reads only its own layer's files, writes its own report, and never
touches the orchestrating conversation's context.

1. Before starting, check `docs/code-review/` for layer report files already
   dated today. If a sweep was interrupted in an earlier session, resume by
   only dispatching subagents for the layers that don't have a report yet for
   today's date — don't redo layers already completed.
2. The remaining applicable layers are independent of each other (no layer's
   review depends on another's findings), so dispatch all of them together:
   one `Agent` call per layer, all issued in the **same message** so they run
   in parallel. Don't dispatch them one at a time and wait in between.
3. Each subagent's prompt must be self-contained — it has no memory of this
   conversation — and must include:
   - The layer key, its directory scope, and its primary checklist sections
     (copy the relevant row from the layer table above into the prompt).
   - An instruction to first read
     `.claude/skills/code-reviewer/SKILL.md` in the repo for the full text of
     those checklist sections, the severity legend, and the "Review Report
     Format & Output File" section (§16) — don't restate the whole checklist
     in the prompt, point the subagent at the file.
   - The exact output path to write:
     `docs/code-review/CODE_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
     (apply the `-2`/`-3` collision rule from §16 itself if the file already
     exists).
   - An explicit instruction to only read/review files under that layer's own
     directory scope — not the whole repo — and to write the report file
     itself rather than just returning findings as chat text.
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

Severity levels used throughout:

| Level             | Meaning                                                                              |
| ----------------- | ------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Breaks correctness, data integrity, or security. Must be fixed before merging.       |
| 🟡 **WARNING**    | Violates a project standard or will cause maintainability problems. Should be fixed. |
| 🟢 **SUGGESTION** | Style preference or minor improvement. Fix if convenient.                            |

---

## 1. Architecture & Layering (both sub-projects)

### 1.1 Backend dependency flow

The backend uses **hexagonal (ports and adapters)** architecture. Allowed imports:

```
infrastructure/web/      → may import application/usecase/, domain/
application/usecase/     → may import domain/ only
domain/                  → must NOT import application/, infrastructure/
infrastructure/persistence/
infrastructure/service/  → may import domain/ only
```

🔴 Flag any class in `domain/` that imports from `application/`, `infrastructure/`,
or any Spring / JPA annotation.

🔴 Flag any controller that contains business logic instead of delegating
immediately to a use-case interface from `domain/port/in/`.

🔴 Flag any JPA `@Entity` class placed in `domain/` — entities belong in
`infrastructure/persistence/entity/`; domain has pure POJOs in `domain/model/`.

🟡 Flag any use-case implementation that injects a Spring Data JPA interface
directly — it must go through a `domain/port/out/` repository interface.

### 1.2 Backend port / adapter split

The project has three port/adapter pairs; all must follow the naming rules:

| Role | Interface location | Naming | Adapter location | Naming |
|------|--------------------|--------|-----------------|--------|
| Use case (driving) | `domain/port/in/<pkg>/` | `FooUseCase` | `application/usecase/<pkg>/` | `FooUseCaseImpl` |
| Service port (driven) | `domain/port/out/` | `FooPort` | `infrastructure/service/` | `FooServiceAdapter` |
| Repository port (driven) | `domain/port/out/` | `FooRepository` | `infrastructure/persistence/adapter/` | `FooRepositoryImpl` |

🔴 Flag any adapter class that is injected directly instead of its port interface.

🔴 Flag any use-case implementation that injects another use-case implementation
directly — use-cases must be composed via port interfaces only.

🟡 Flag any port interface that lives outside `domain/port/in/` or `domain/port/out/`.

🟡 Flag a port/adapter pair whose adapter does nothing but delegate to
*another* port for the same capability (e.g. `FooAdapter.doThing()` just
calls `barPort.doThing()`) — that's not a real abstraction, it's a
pass-through. The test for whether it should exist is whether the adapter
contributes any logic of its own — **not** whether something currently calls
it. Having production callers doesn't save a pure-delegation wrapper: delete
the port and adapter, and repoint every caller to depend on the port/adapter
that actually implements the behavior instead. Only keep the pair if the
adapter does real, non-trivial work beyond forwarding the call. This is a
cross-cutting antipattern, not backend-specific — the same test applies to
any delegate-only wrapper in either sub-project (see §10.2 for the frontend
form) — see the `HashCalculatorPort` / `AssetHashCalculatorAdapter` and
`AudioPlayerService` entries in §15.

### 1.3 Frontend layer rules

```
core/       → services and models only; no UI components
features/   → page-level smart components; calls core services and shared components
shared/     → pure presentational components and pipes; no service calls
```

🔴 Flag any component in `shared/` that injects or calls a service directly.

🟡 Flag any service placed under `features/` — it belongs in `core/services/`.

---

## 2. Backend: Spring Proxy Pitfalls

This is the most commonly misunderstood area in the codebase.

### 2.1 `@Transactional` / `@Async` self-invocation

Spring applies `@Transactional` and `@Async` via a proxy. Calling a method on
`this` bypasses the proxy — the annotation has **no effect**.

🔴 Flag any method annotated `@Transactional` or `@Async` that is called from
another method in the **same class** (i.e., via `this.foo()` or just `foo()`).

The fix is always to extract the callee into a separate `@Service` bean and
inject it.

**Wrong:**

```java
@Service
public class FooServiceImpl implements FooService {
    @Async
    public CompletableFuture<Void> doWork() {
        processItem(); // self-invocation — @Transactional on processItem is ignored
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    protected void processItem() { ... } // never fires
}
```

**Right:**

```java
@Service
public class FooServiceImpl implements FooService {
    private final BarService barService; // separate bean

    @Async
    public CompletableFuture<Void> doWork() {
        barService.processItem(); // proxy intercepts → @Transactional fires
        return CompletableFuture.completedFuture(null);
    }
}
```

### 2.2 Detached entities from missing transactions

When `@Transactional` is missing (often due to self-invocation), each
repository call runs in its own micro-transaction. Entities returned from one
call are **detached** before the next call starts. Persisting a new entity
that references a detached entity may cause silent failures or
`DetachedObjectException`.

🔴 Flag patterns where repository calls are made in sequence without a
wrapping transaction and entities from one call are passed to another.

---

## 3. Backend: Annotations & Boilerplate

### 3.1 Lombok

🟡 Flag any manually written getter, setter, constructor, or `toString` that
Lombok could generate:

| Anti-pattern                               | Replacement                |
| ------------------------------------------ | -------------------------- |
| Hand-written getters/setters on entity/DTO | `@Data`                    |
| Explicit no-args constructor on entity     | `@NoArgsConstructor`       |
| Explicit all-args constructor on service   | `@RequiredArgsConstructor` |
| `private static final Logger log = ...`    | `@Slf4j`                   |

### 3.2 Dependency injection

🔴 Flag `@Autowired` on a field — use constructor injection via
`@RequiredArgsConstructor` instead.

🟡 Flag `@Autowired` on a constructor that Lombok's `@RequiredArgsConstructor`
could generate.

### 3.3 Logging

🔴 Flag any `System.out.println` or `System.err.println`.

🟡 Flag log statements that don't include enough context (entity ID, file
path, etc.) to diagnose a production failure.

🟡 Flag `e.printStackTrace()` — use `log.error("...", e)` instead.

### 3.4 OpenAPI controller annotations

Every `@RestController` in this project **must** carry OpenAPI annotations.
`springdoc-openapi-starter-webmvc-ui` is on the classpath and Swagger UI is
served at `/swagger-ui.html`.

🟡 Flag any `@RestController` class that is missing a class-level
`@Tag(name = "...", description = "...")` annotation.

🟡 Flag any `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping`
/ `@DeleteMapping` method that is missing `@Operation(summary = "...")`.

🟡 Flag any endpoint method that is missing `@ApiResponses` with at least one
`@ApiResponse` per reachable HTTP status code.

Expected import block for every annotated controller:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

---

## 4. Backend: Transactions

🔴 Flag write operations (save, delete, update) in a facade or service method
that lacks `@Transactional`.

🟡 Flag read-only facade methods that lack `@Transactional(readOnly = true)`.

🟡 Flag `open-in-view` set to `true` in `application.yml` — it must stay
`false`; load lazy associations within transactions.

🔴 Flag any lazy association (`@ManyToOne(fetch = LAZY)`, `@OneToMany`) that
is accessed outside a transaction — this causes `LazyInitializationException`.

🟡 Flag `@OneToMany` or `@ManyToOne` without `fetch = FetchType.LAZY`.

---

## 5. Backend: JPA Entities

🔴 Flag any entity that is missing `@NoArgsConstructor` (Hibernate requires
a no-args constructor).

🟡 Flag bean-validation annotations (`@NotBlank`, `@NotEmpty`, `@Size`) on an
entity field — they belong on API DTOs, not entities.

🟡 Flag any `CascadeType.ALL` or `CascadeType.REMOVE` on a `@ManyToOne`
relationship — cascading deletes up to a parent is almost never correct.

🟡 Flag any `@Column(nullable = false)` that doesn't have a corresponding
`NOT NULL` in the Flyway migration, or vice versa.

---

## 6. Backend: Database Migrations

🔴 Flag any modification to an existing Flyway migration file (those in
`src/main/resources/db/migration/`). Applied migrations must never change;
create a new `V{n+1}__*.sql` instead.

🔴 Flag a new `@Column` added to an entity that has no corresponding column
in the migrations (schema will drift if `ddl-auto` is `none` or `validate`).

🟡 Flag migration filenames that don't follow `V{n}__{Description}.sql`
(two underscores, sequential version number).

---

## 7. Backend: Async & SSE

🔴 Flag an `@Async` method that does **not** return `CompletableFuture<T>` —
the async execution machinery needs the future to report completion or errors.

🟡 Flag a long-running operation that blocks a web thread instead of using
`@Async` + `SseEmitter`.

🟡 Flag any `SseEmitter` usage that doesn't call `emitter.complete()` or
`emitter.completeWithError()` at the end of the operation — the connection
will hang open.

🔴 Flag any `SecurityFilterChain` that does **not** include
`.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as the first
authorisation rule. Without it, Tomcat's async dispatch thread (used by
`SseEmitter`) re-runs the Spring Security filter chain without a
`SecurityContext` and throws `AuthorizationDeniedException` — "response is
already committed". This must come before all other `requestMatchers` rules.

---

## 8. Backend: Naming Conventions

| Element              | Expected                                                          | Example                              |
| -------------------- | ----------------------------------------------------------------- | ------------------------------------ |
| Class                | PascalCase                                                        | `CatalogAssetsUseCaseImpl`           |
| Method               | camelCase                                                         | `execute()`, `findByFolder()`        |
| Field / variable     | camelCase                                                         | `folderRepository`, `storagePort`    |
| Constant             | UPPER_SNAKE_CASE                                                  | `THUMBNAIL_MAX_WIDTH`                |
| Enum value           | UPPER_SNAKE_CASE                                                  | `ASSET_CREATED`                      |
| Test class           | `{Class}Test` or `{Class}Tests`                                   | `CatalogAssetsUseCaseImplTest`       |
| Test method          | `method_condition_expected`                                       | `execute_folderExists_returnsAssets` |
| Migration            | `V{n}__{Description}.sql`                                         | `V2__Add_hash_column.sql`            |
| Use-case interface   | `FooUseCase` in `domain/port/in/<pkg>/`                           | `CatalogAssetsUseCase`               |
| Use-case impl        | `FooUseCaseImpl` in `application/usecase/<pkg>/`                  | `CatalogAssetsUseCaseImpl`           |
| Service port         | `FooPort` in `domain/port/out/`                                   | `StoragePort`, `ThumbnailPort`       |
| Service adapter      | `FooServiceAdapter` in `infrastructure/service/`                  | `StorageServiceAdapter`              |
| Repository port      | `FooRepository` in `domain/port/out/`                             | `AssetRepository`, `FolderRepository`|
| Repository adapter   | `FooRepositoryImpl` in `infrastructure/persistence/adapter/`      | `AssetRepositoryImpl`                |
| JPA repository       | `JpaFooRepository` in `infrastructure/persistence/jpa/`           | `JpaAssetRepository`                 |
| JPA entity           | `FooEntity` in `infrastructure/persistence/entity/`               | `AssetEntity`, `FolderEntity`        |
| Domain model         | Plain class in `domain/model/`                                    | `Asset`, `Folder`                    |
| HTTP request DTO     | `FooRequestDto` in `infrastructure/web/dto/request/`              | `CreateAlbumRequestDto`              |
| HTTP response DTO    | `FooResponseDto` in `infrastructure/web/dto/response/`            | `AssetResponseDto`                   |
| HTTP shared DTO      | Unchanged name in `infrastructure/web/dto/shared/`                | `UserPreferenceDto`                  |

🟡 Flag any violation of the above.

🔴 Flag any class placed directly in `infrastructure/web/dto/` instead of one of
its `request/`, `response/`, or `shared/` subpackages.

🟡 Flag a request DTO (used only as `@RequestBody`/`@RequestParam`) that isn't
named `{BaseName}RequestDto`, or a response DTO (used only as a `ResponseEntity<...>`/
return-type payload, including nested inside another response DTO) that isn't named
`{BaseName}ResponseDto`. A DTO belongs in `shared/` only if the exact same class is
verified to appear as both a request and a response payload across every controller
method that references it — don't place it there just because the name is ambiguous.

---

## 9. Backend: Testing

🔴 Flag unit tests that use `@SpringBootTest` — unit tests must use
`@ExtendWith(MockitoExtension.class)` only.

🟡 Flag tests that don't name the system under test `sut`.

🟡 Flag tests that use JUnit's `assertEquals` / `assertTrue` — use AssertJ's
`assertThat(...)` instead.

🟡 Flag integration tests that don't carry `@ActiveProfiles("test")` — they
may run against the real database.

🟡 Flag test methods with more than one `assertThat` that tests a different
concept — split into separate test methods.

---

## 10. Frontend: Angular Conventions

### 10.1 Component structure

🔴 Flag any `NgModule` — all components must be `standalone: true`.

🔴 Flag `*ngIf` or `*ngFor` in templates — use `@if` / `@for` (Angular 17+
control flow) instead.

🟡 Flag `@for` without a `track` expression.

🟡 Flag a component's `imports: []` that includes a module the template
doesn't actually use.

🟡 Flag a component that subscribes to observables in the constructor instead
of `ngOnInit`.

🔴 Flag a component that opens an `EventSource` but doesn't close it in
`ngOnDestroy`.

### 10.2 Services

🔴 Flag a service that subscribes to an `Observable` internally — services
must return `Observable<T>` and let the component subscribe.

🟡 Flag a service without `providedIn: 'root'`.

🟡 Flag HTTP logic inside a component — API calls belong in `core/services/`.

🟡 Flag a service file that contributes no logic of its own and just
re-exports or aliases another service (e.g.
`export { FooService as BarService } from './foo.service';`, or a class whose
every method is a one-line passthrough to an injected service of a different
name for the same capability). Same test as the backend port/adapter case in
§1.2: the question is whether it adds logic, not whether something imports
it. Delete it and repoint any importers to the real service — see the
`AudioPlayerService` entry in §15.

### 10.3 Routing

🟡 Flag a new feature route that is not lazy-loaded with `loadComponent`.

🟡 Flag a direct import of a feature component in `app.routes.ts` instead of
a dynamic import.

---

## 11. Frontend: TypeScript Conventions

🔴 Flag any use of `any` — use a typed interface, `unknown` with a type
guard, or `Partial<T>`.

🔴 Flag `console.log` / `console.error` left in committed code — use
`MatSnackBar` for user-facing feedback and remove debug output.

🟡 Flag `!` (non-null assertion) that could be replaced with optional
chaining (`?.`) or a null check.

🟡 Flag a property initialised as `undefined` that could be typed as optional
(`field?: Type`).

🟡 Flag an interface defined inline in a component — move it to
`core/models/`.

---

## 12. Frontend: Naming Conventions

| Element            | Expected                   | Example                                    |
| ------------------ | -------------------------- | ------------------------------------------ |
| File               | `kebab-case.<type>.ts`     | `asset.service.ts`, `gallery.component.ts` |
| Class              | PascalCase                 | `GalleryComponent`, `AssetService`         |
| Interface          | PascalCase                 | `Asset`, `PaginatedData`                   |
| String union       | UPPER_SNAKE_CASE           | `'FILE_NAME' \| 'FILE_SIZE'`               |
| Property / method  | camelCase                  | `currentFolder`, `loadAssets()`            |
| Component selector | `app-kebab-case`           | `app-gallery`                              |
| Test suite         | `describe('ClassName')`    | `describe('GalleryComponent')`             |
| Test case          | `it('should <behaviour>')` | `it('should display thumbnails')`          |

🟡 Flag any violation of the above.

---

## 13. Frontend: Cypress Tests

🔴 Flag Cypress tests that use Jasmine matchers (`toBe`, `toEqual`) — use
Chai assertions (`expect(...).to.equal(...)`, `cy.get(...).should(...)`).

🔴 Flag component tests missing `provideNoopAnimations()` — Angular Material
animations cause flaky failures in Cypress.

🟡 Flag service stubs typed as `any` — use `Partial<ServiceType>`.

🟡 Flag `EventSource` usage in a test without `MockEventSource` — real SSE
connections must not be opened in component tests.

🟡 Flag test files placed outside the source tree (e.g., in a top-level
`tests/` folder) — test files must be co-located with their source files as
`*.cy.ts`.

🟡 Flag a `describe` block with no `beforeEach` that repeats the same
`cy.mount()` call in every `it` — extract to `beforeEach`.

---

## 14. Cross-cutting: Comments & Code Style

🟢 Flag comments that restate what the code does (e.g., `// increment counter`
before `counter++`). Only comments explaining **why** (a non-obvious
constraint, workaround, or invariant) are kept.

🟢 Flag multi-line comment blocks or Javadoc on internal methods — terse
single-line comments are preferred, and only when necessary.

🟢 Flag large imperative loops that could be replaced by a stream or array
method.

---

## 15. Known Project-Specific Pitfalls

These have caused real bugs in this codebase and deserve extra attention:

| Pitfall                                    | What to look for                                                                                                                                 |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `@Transactional` self-invocation           | Method annotated `@Transactional` called from same class without going through proxy                                                             |
| JPA entity in `domain/`                    | `@Entity` class placed under `domain/model/` or `domain/port/` — entities belong in `infrastructure/persistence/entity/`                        |
| Adapter injected directly                  | Caller injects `FooRepositoryImpl` or `FooServiceAdapter` instead of the `domain/port/out/` interface                                           |
| Use case bypassing port interface          | Use-case impl injects another use-case impl directly instead of its `domain/port/in/` interface                                                  |
| Missing catalog directory                  | `root-catalog-folders` pointing to a directory that may not exist on all machines; should use `application-local.yml` for machine-specific paths |
| Lazy association outside transaction       | Accessing `asset.getFolder()` or similar after the session is closed                                                                             |
| SSE emitter not completed                  | `SseEmitter` left open after the async operation finishes                                                                                        |
| `@Async` without `@EnableAsync`            | `@Async` has no effect if `@EnableAsync` is missing from the application class                                                                   |
| Cypress Jasmine matchers                   | Using `toBe` / `toEqual` instead of Chai `to.equal` / `to.deep.equal`                                                                            |
| `*ngIf` / `*ngFor` in new templates        | Angular 17+ control flow must be used; directives are legacy                                                                                     |
| SSE + Spring Security async dispatch       | `SecurityFilterChain` missing `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` → `AuthorizationDeniedException` on the async thread   |
| Token in URL for SSE / image endpoints     | `EventSource` and `<img>` do not send `Authorization` headers; tokens in query params appear in logs — use HttpOnly cookies instead              |
| SecurityConfig circular dependency         | `@Bean UserDetailsService` defined in `SecurityConfig` while `SecurityConfig` injects a filter that depends on it — extract to separate config   |
| JPA delete-then-insert with stale IDs      | `deleteAll()` + `saveAll(incoming)` when incoming entities still have old IDs → Hibernate merges against deleted rows; use `deleteAllInBatch()` + `setId(null)` |
| CORS missing `PATCH`                       | `AppConfig.corsFilter()` `allowedMethods` omitting `"PATCH"` when `@PatchMapping` endpoints exist → 403 on preflight                            |
| Missing OpenAPI annotations on controller  | New `@RestController` added without `@Tag` / `@Operation` / `@ApiResponses` — controller appears in Swagger UI under "default" with no documentation |
| Hand-written mapper                        | Entity ↔ domain model or HTTP DTO ↔ domain model conversion done manually instead of with a MapStruct `@Mapper(componentModel = "spring")`      |
| DTO placed directly in `web/dto/`          | New HTTP DTO added straight to `infrastructure/web/dto/` instead of its `request/`, `response/`, or `shared/` subpackage, or named without the `RequestDto`/`ResponseDto` suffix |
| Delegate-only port/adapter or service      | A port/adapter (backend) or service (frontend) with no logic of its own, just forwarding to another one for the same capability. The keep-or-delete test is whether it contributes its own logic — **not** whether it currently has callers; existing callers just mean they need repointing to the real implementation, not that the wrapper earns a reprieve. Backend incident: `HashCalculatorPort`/`AssetHashCalculatorAdapter` duplicated `StoragePort.computeHash`'s SHA-256 logic; the first fix made the adapter delegate to `StoragePort` instead of deleting it and migrating callers — the pair was pure pass-through and should have been deleted outright, with any real callers repointed to `StoragePort` directly. Frontend incident: `core/services/audio-player.service.ts` was a bare re-export (`export { MediaPlayerService as AudioPlayerService } from './media-player.service'`) with zero importers anywhere in the codebase — deleted outright |

---

## 16. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Review: <file or PR title>

### 🔴 Critical
- `path/to/File.java:42` — <issue description>

### 🟡 Warnings
- `path/to/file.ts:15` — <issue description>

### 🟢 Suggestions
- `path/to/File.java:88` — <issue description>

### Verdict
<One or two sentences: overall quality, whether it is safe to merge, and the
single most important thing to fix first.>
```

If there are no findings in a severity category, omit that category entirely.

### Write the report to a dated markdown file

Every time this skill runs — full-codebase sweep, single file, or PR review —
also write the findings to a new markdown file so work can be resumed later
without re-deriving context.

**Scoped review (single file, PR, feature, or one sub-project) — one file:**

- **Path:** `docs/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}.md` (repo
  root, today's date, ISO 8601). If a file for that date already exists (e.g.,
  a second review the same day), append `-2`, `-3`, etc. before `.md` rather
  than overwriting the earlier run's report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:** `docs/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
  where `{layer}` is the report suffix from the layer table (e.g.
  `backend-domain`, `frontend-features`, `cross-cutting`). Same `-2`, `-3`
  collision rule, applied per date+layer combination.
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

---

## 17. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new review.
It is interactive and incremental: fix a chunk, check it off, ask what's next.
**This workflow never commits** — all changes stay uncommitted in the working
tree for the user to review and commit themselves.

### 17.1 Locate the report(s) for a date

1. If the user names a specific report file, skip straight to §17.2 with that
   file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `docs/code-review/CODE_REVIEW_FINDINGS_*.md` file. If none exists, say so
   and stop — there is nothing to fix.
2. List every report file for that date (there may be several `-2`/`-3` reruns
   per layer — treat each filename, suffix included, as a distinct report).
   For each, quickly check whether it has any unchecked (`- [ ]`) boxes left;
   drop fully-checked-off reports from the list.
3. **If exactly one report remains**, use it directly — don't make the user
   pick from a list of one. **If more than one remains** (the normal case
   after a layer-split full-codebase sweep, or several same-day scoped
   reviews), ask the user which report to work on first, showing the layer
   name (or scope, for a pre-layer-split report) and a rough Critical/Warning/
   Suggestion count for each so they can prioritize. Work on exactly one
   report at a time — don't mix findings from two reports into a single fix
   pass.

### 17.2 Read the report, then ask what to fix

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

### 17.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected file(s) and understand the finding in the full context
   of the surrounding code before changing anything — the report is a
   pointer, not a substitute for reading the code.
2. Apply the fix. The report tells you what's wrong; the checklist sections
   above (1–15) tell you what "right" looks like for that category of issue.
3. If a fix hinges on a real design decision rather than just applying a
   known pattern — e.g. a live-data/migration-compatibility risk, a public
   API/contract change, or several equally valid approaches — stop and ask
   the user before proceeding instead of picking one unilaterally.
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
     investigation, the right call is to leave the code as-is. Document why
     so the item isn't silently dropped.
   Updating the report per-finding (not at the end of the whole scope) means
   an interrupted session still leaves an accurate resume point.
7. If fixing one finding incidentally resolves another still-unchecked one
   (e.g. moving a domain model also fixes a naming-convention Warning on the
   same class), check that one off too with a note explaining it was fixed as
   a byproduct — don't leave it unchecked just because it wasn't the primary
   target.

### 17.4 Continue until done

After finishing the selected scope, run the widest verification available
(full backend suite, full frontend Cypress suite, lint, production build)
once before asking what's next — don't let per-finding narrow tests substitute
for a full-suite check when a scope is done. Then:

- If the current report still has unchecked findings, repeat from §17.2: ask
  what to fix next **within the same report**, offering only what's still
  unchecked.
- If the current report is now fully checked off, go back to §17.1 — re-list
  the remaining reports for the date (if this was a layer-split sweep, there
  are likely others) and ask the user whether to move to another one or stop.

Stop when the user says to stop, or when every report for the date is fully
checked off.

### 17.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command as
part of this workflow, not even implicitly. Leave all changes uncommitted so
the user can review the diff and commit it themselves.
