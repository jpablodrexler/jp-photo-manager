---
name: code-reviewer
description: >
  Code review skill for the JPPhotoManager project (Spring Boot 3.4 / Java 21
  backend + Angular 19 frontend). Use when asked to review, audit, or check
  code for correctness, style, or standards compliance — whether that means
  reviewing a pull request, checking a file before committing, validating that
  a new class follows project conventions, or assessing the quality of a change
  that was just written. Covers both sub-projects (backend and frontend) and
  all cross-cutting concerns: architecture, naming, transactions, async,
  testing, and TypeScript/Java style rules.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Code Reviewer Skill

Review code in the JPPhotoManager project against its documented architecture,
conventions, and known pitfalls. The project has two sub-projects with distinct
stacks; apply the relevant checklist(s) based on which files are under review.

## Workflow

1. Identify which sub-project(s) are affected: **backend** (Java), **frontend**
   (Angular/TypeScript), or both.
2. Read every changed file before forming any opinion.
3. Work through the relevant checklist(s) below, section by section.
4. Report findings grouped by **severity**, then by file.
5. Summarise with a short verdict and the top action items.

Severity levels used throughout:

| Level             | Meaning                                                                              |
| ----------------- | ------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Breaks correctness, data integrity, or security. Must be fixed before merging.       |
| 🟡 **WARNING**    | Violates a project standard or will cause maintainability problems. Should be fixed. |
| 🟢 **SUGGESTION** | Style preference or minor improvement. Fix if convenient.                            |

---

## 1. Architecture & Layering (both sub-projects)

### 1.1 Backend dependency flow

Allowed imports:

```
api/        → may import application/, domain/
application/ → may import domain/
domain/     → must NOT import api/, application/, infrastructure/
infrastructure/ → may import domain/
```

🔴 Flag any class in `domain/` that imports from `api/`, `application/`, or
`infrastructure/`.

🔴 Flag any controller that contains business logic instead of delegating
immediately to `PhotoManagerFacade`.

🟡 Flag any facade method that contains business logic instead of delegating
to domain services or repositories.

### 1.2 Backend service interface / implementation split

Every service **must** exist as two files:

| File                  | Package                   | Role                               |
| --------------------- | ------------------------- | ---------------------------------- |
| `FooService.java`     | `domain/service/`         | Interface only — no implementation |
| `FooServiceImpl.java` | `infrastructure/service/` | `@Service implements FooService`   |

🔴 Flag any `@Service` class in `infrastructure/service/` that does not implement
a corresponding interface in `domain/service/`.

🔴 Flag any caller (facade, controller, other service) that injects
`FooServiceImpl` instead of the `FooService` interface.

🟡 Flag any service interface that lives outside `domain/service/`.

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

| Element           | Expected                                      | Example                              |
| ----------------- | --------------------------------------------- | ------------------------------------ |
| Class             | PascalCase                                    | `CatalogAssetsServiceImpl`           |
| Method            | camelCase                                     | `catalogAssetsAsync()`               |
| Field / variable  | camelCase                                     | `folderRepository`                   |
| Constant          | UPPER_SNAKE_CASE                              | `THUMBNAIL_MAX_WIDTH`                |
| Enum value        | UPPER_SNAKE_CASE                              | `ASSET_CREATED`                      |
| Test class        | `{Class}Test` or `{Class}Tests`               | `CatalogFolderServiceImplTest`       |
| Test method       | `method_condition_expected`                   | `catalogFolder_newFile_createsAsset` |
| Migration         | `V{n}__{Description}.sql`                     | `V2__Add_hash_column.sql`            |
| Service interface | `FooService` in `domain/service/`             | `CatalogFolderService`               |
| Service impl      | `FooServiceImpl` in `infrastructure/service/` | `CatalogFolderServiceImpl`           |

🟡 Flag any violation of the above.

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

---

## 16. Review Report Format

Structure the output as follows:

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
