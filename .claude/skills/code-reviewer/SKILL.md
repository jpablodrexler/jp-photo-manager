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
| Cross-cutting             | `cross-cutting`           | Both sub-projects; no single directory                                                                                                    | §9, §13, §14, §18, §19, §20, §21, §22, delegate-only port/adapter or service pairs (§1.2/§10.2), dependency-direction violations, systemic naming patterns |

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

1. Before starting, check `docs/reports/code-review/` for layer report files already
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
     `docs/reports/code-review/CODE_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
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

### 10.4 Material Component Layout Gotchas

🟡 Flag a `mat-icon` placed inside `mat-card-avatar` with no matching CSS
rule sizing and centering it (`width`/`height`, `font-size`/`line-height`,
flex-centered). `mat-card-avatar`'s built-in sizing (`object-fit: cover`,
`overflow: hidden`) is designed for an `<img>` — left unstyled for a
`mat-icon`, the glyph renders oversized and gets clipped by the avatar
circle down to an unrecognizable fragment.

🟢 Flag a `mat-form-field` placed as the very first element in
`mat-card-content`, directly under a `mat-card-title` with nothing else
between them, that has no `margin-top` of its own. The field's
floating-label notch plus `mat-card-header`'s tight bottom padding tends to
read as the field crowding the title above it.

🟡 Flag the app shell's root `mat-toolbar` if it stays pinned on screen by
neither of this app's two valid mechanisms: an explicit `position: fixed`/
`sticky` rule with a matching sibling offset (e.g. a `margin-top` equal to
the toolbar's height, so taking it out of flow doesn't overlap the content
below it), *or* — the mechanism `app.component.scss` actually uses — being
a normal-flow flex-column sibling of a `flex: 1; min-height: 0;`,
independently-`overflow-y: auto` content container, so only that container
scrolls and the toolbar (never taken out of flow at all) never moves.
`<mat-toolbar>` has no built-in pinning of its own — a toolbar with
neither mechanism applied sits in normal document flow and scrolls out of
the viewport with page content.

🟢 Flag a **new** app-shell, dialog, or full-page layout that scrolls the
whole document under a `position: fixed`/`sticky` header instead of
confining scroll to a `flex: 1; min-height: 0; overflow-y: auto` content
container the way `app.component.scss` already does — a document-level
scrollbar then spans the full viewport height and runs past the header's
own boundary. Prefer a `height: 100vh`/`100dvh` flex column with the
header as a normal-flow sibling above the scrolling content region; only
reach for `position: sticky`/`fixed` plus a compensating offset if
document-level scroll is genuinely intended elsewhere on the same page.
This applies to newly-introduced scroll regions and shell restructures —
the app's existing shell already follows the correct pattern, so don't
re-flag it on an unrelated change.

🟡 Flag a row-like flex container (a list row, a card's action bar, any
element packing an icon/name/secondary-text/count/buttons on one line)
whose CSS gives it `display: flex` with no `flex-wrap` and no
narrow-viewport fallback (a media query, a container query, or a
restructure into a two-line layout below a breakpoint). At full width
this looks fine; at a real phone width the flex children fight over too
little space, and a shrinking text child can visually overlap a
fixed-width sibling instead of the row wrapping cleanly. Confirm the
change was actually checked at `angular-developer` §20's standard mobile
check device, not just eyeballed at desktop width or a wider handset
preset.

🟢 Flag a `flex-wrap: wrap` action-button row (a toolbar of several
buttons) with no shared width rule on its buttons. Wrapping alone stops
the horizontal-scroll problem, but each button still sizes to its own
icon+label content by default — once a narrow viewport forces one button
per line, a short label ends up visibly narrower than a long one, reading
as a ragged, unpolished column. The buttons should share a uniform width
once wrapped — stacking each full-width is the simplest correct fix.

🟡 Flag an outline `mat-form-field` whose floating `mat-label` can be
**clipped or truncated**:
- Placed as the first element inside a scrollable container (`mat-dialog-
  content`, or any `overflow: auto`/`hidden` region) with no `margin-top`
  on the field or its row — the label sits a few pixels above the field's
  border box, and the container clips it at its padding box once the
  content scrolls. This is the scroll-clipping counterpart of the
  `mat-card-content` crowding flag above.
- Hard-sized narrower than its label needs (`width`/`flex: 0 0 <fixed>`
  under a longer `mat-label`) — the label renders with an ellipsis. A
  labelled field should use `min-width` plus a growable `flex`, size to
  content, or carry a shorter label instead.

🟡 Flag a `mat-form-field` bound to a `FormControl` with validators —
typically a catalog/dialog **add row** or an otherwise-optional field —
that surfaces its error state on a bare focus-then-blur, before the user
has attempted the submit/add. Angular Material's default
`ErrorStateMatcher` fires on `invalid && touched`, Material marks a
control `touched` on blur, and `MatDialog` auto-focuses the first field on
open — so any later click trips a "required" error the user never
provoked. Look for a template `<mat-error>` gated on `control.touched`
(rather than an explicit "attempted" signal), and for a validated add-row
field with **no** custom `[errorStateMatcher]` (the `<mat-error>` `@if`
alone doesn't keep the field's own red outline/label out of the error
state). The fix pattern: an `xAttempted` signal set only on an invalid
submit, reset after success, gating both the `@if` and a per-field
matcher.

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

🟡 Flag a test that mutates a component's plain (non-signal) field directly
(e.g. calling a component method from test code that sets a field, then
asserting on the resulting DOM state) via only `fixture.detectChanges()`
— in this zoneless app that needs an explicit
`fixture.componentRef.injector.get(ChangeDetectorRef).markForCheck()`
first, or the view is never rechecked.

🟡 Flag `import { mount } from 'cypress/angular'` in a test file — `cy.mount`
is a global command already registered in `cypress/support/component.ts`;
a test file should never import `mount` directly.

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
| Method/function past the complexity threshold | `mvn pmd:check` (backend) or `npm run complexity` (frontend) reports something over 15 — see §18 |
| Line coverage below 80%                    | `mvn jacoco:check` (backend) or `npm run coverage:check` (frontend) reports under 80% — see §19 |
| Untyped (`any`) identifier (frontend)      | `npm run type-coverage:report` lists it — see §20 |
| Unused export/file/dependency              | `npm run dead-code:report` (frontend) or `mvn dependency:analyze` (backend) lists it — see §21 |
| Survived mutant (test runs, doesn't assert) | `npm run mutation:report` (frontend) or `bash scripts/mutation-report.sh` (backend) lists it — see §24 |
| `mat-form-field` errors on blur before any submit attempt | An add-row/optional validated field showing its "required" error on focus-then-blur — gate on an explicit "attempted" signal + per-field `[errorStateMatcher]`, not `control.touched` — see §10.4 |
| Outline `mat-form-field` label clipped or truncated | First field in a scrollable container with no `margin-top` (label clipped at the container edge), or a field hard-sized narrower than its `mat-label` (label ellipsised) — see §10.4 |
| Flex row overlaps or a wrapped button row is ragged at mobile width | Not checked at `angular-developer` §20's standard mobile check device — see §10.4 |

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

- **Path:** `docs/reports/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}.md` (repo
  root, today's date, ISO 8601). If a file for that date already exists (e.g.,
  a second review the same day), append `-2`, `-3`, etc. before `.md` rather
  than overwriting the earlier run's report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:** `docs/reports/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
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
   `docs/reports/code-review/CODE_REVIEW_FINDINGS_*.md` file. If none exists, say so
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
   above (1–15, 18) tell you what "right" looks like for that category of
   issue.
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

---

## 18. Cyclomatic Complexity (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a McCabe cyclomatic
complexity pass, in addition to the manual checklists above. Complexity is
measured per method/function (each starts at 1; +1 for each `if`, ternary,
loop, `catch`/switch-`case`, and short-circuit operator — `&&`, `||`, `??`
on the frontend; PMD's equivalent counting on the backend). **Max allowed
complexity is 15 per method/function**, in both sub-projects.

Don't count this by hand — each sub-project has its own checked-in analyzer.

### 18.1 Frontend (TypeScript)

Run from `frontend/`:

```
npm run complexity
```

(equivalent to `node scripts/cyclomatic-complexity.js src/app`, which walks
every non-`.cy.ts` `.ts` file under a given directory via the TypeScript
compiler API — see `frontend/scripts/cyclomatic-complexity.js`, same
decision-point rules as ESLint's built-in `complexity` rule). It exits
non-zero and lists every offending function (file, line, name, complexity)
when anything exceeds the threshold. For a scoped review, either run it
against the whole tree and filter the output to the changed files, or pass a
narrower directory directly, e.g.
`node scripts/cyclomatic-complexity.js src/app/features/gallery`.

### 18.2 Backend (Java)

Run from `backend/`:

```
mvn pmd:check
```

This invokes the `maven-pmd-plugin` (configured in `backend/pom.xml`,
version 3.28.0, bundling PMD 7.17.0) against `backend/pmd-complexity-ruleset.xml`,
which enables only PMD's built-in `CyclomaticComplexity` rule with
`methodReportLevel` set to 16 (PMD reports a violation when complexity is
**greater than or equal to** the configured level, so 16 is what flags
"over 15") and `classReportLevel` effectively disabled — this check is
scoped to individual methods, not a class's combined total. The plugin is
declared with no `<executions>` binding, so it never runs as part of the
normal build/test/CI lifecycle (`mvn verify`, `mvn package`, ...) — it's
opt-in, invoked only when this check is run, the same way the frontend's
`npm run complexity` isn't part of `npm run build`/`test`. `mvn pmd:check`
fails the command (non-zero exit) and writes `target/pmd.xml` when anything
exceeds the threshold — read that file, or the console output, for the
offending class/method/line.

### 18.3 Flagging

🟡 Flag any method/function reported over complexity 15 — this is a
maintainability problem (deep, hard-to-test branching), not a correctness
bug, so it's a WARNING rather than CRITICAL, but it should be fixed: extract
guard clauses, split the method/function by responsibility, or replace a
long `if`/`else if` chain with a lookup table/strategy map (backend:
consider a `switch` on an enum, a `Map<Key, Handler>`, or splitting the
use-case into smaller collaborators respecting §1.1/§1.2's port boundaries).
Note the reported complexity number and location in the finding so a fix
can be verified by re-running the relevant command.

🟢 A method/function in the 10–15 range is worth a passing mention if an
obvious, low-effort split exists, but isn't required to be flagged — the
threshold that matters is 15.

### 18.4 Trending snapshots

For a full-codebase sweep, both the pass/fail gates above have a trending
companion that ranks every function/method by complexity instead of only
flagging the ones over threshold — useful for spotting something climbing
toward 15 before it becomes an actual violation.

- **Frontend:** `npm run complexity:report` (`scripts/complexity-report.js`)
  — dated snapshot under `docs/reports/complexity/`, top 20 functions by
  complexity plus top 20 files by line count.
- **Backend:** `bash scripts/complexity-report.sh` (run from `backend/`) —
  same shape, under `JPPhotoManagerWeb/docs/reports/complexity/`. Uses a
  second, report-only `maven-pmd-plugin` execution
  (`pmd-complexity-report-ruleset.xml`, `methodReportLevel=1` so PMD
  reports every method instead of only violations) — never touches the
  real gate's `pmd-complexity-ruleset.xml` or its threshold.

---

## 19. Code Coverage (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a line-coverage
pass, in addition to the manual checklists above. **Minimum line coverage is
80%**, in both sub-projects, whether the check runs over the whole project
or is scoped to just the files a change touched.

Don't estimate this by eye — each sub-project already has a coverage tool
wired (`cypress-unit-test-developer` §1.3; `java-unit-test-developer` §1);
this section only adds the enforced threshold and the two ways to scope the
check.

### 19.1 Frontend (TypeScript)

Run from `frontend/` (collect coverage, then check the threshold):

```
npm run test:coverage
npm run coverage:check
```

`test:coverage` (`cypress run --component --env coverage=true`) re-runs the
component suite with `babel-plugin-istanbul` instrumentation active and
writes `html`/`lcov`/text-summary reports to `coverage/` (gitignored).
`coverage:check` (`nyc check-coverage`) reads the `lines`/`branches`/
`functions`/`statements` thresholds (80 each) from `.nycrc.json` and fails
(non-zero exit) if any falls short.

For a **scoped review** (a single component, service, or feature directory
rather than the whole frontend), don't rely on the whole-project number —
narrow the check with `--include`, which filters the already-collected
coverage map down to matching paths before the threshold is evaluated:

```
npx nyc check-coverage --include "src/app/features/albums/**" --lines 80 --branches 80 --functions 80 --statements 80
```

(`test:coverage` still needs to have run first — `--include` only filters
which already-collected files count toward the ratio, it doesn't limit
which specs execute.)

### 19.2 Backend (Java)

Run from `backend/` (populate `target/jacoco.exec`, then check the threshold):

```
mvn test
mvn jacoco:check
```

This invokes the `jacoco-maven-plugin` (configured in `backend/pom.xml`)
against its default rule — a `BUNDLE`-level `LINE` `COVEREDRATIO` minimum
of `${jacoco.check.minimum}` (80%) — reading the exec data `mvn test`
already produced via the plugin's existing `prepare-agent` execution. Like
`mvn pmd:check` (§18.2), the `check` goal has no `<executions>` binding in
the pom, so it never runs as part of the normal build/test lifecycle
(`mvn test`, `mvn verify`, `mvn package`) — it's opt-in, invoked only when
this check is run. `mvn jacoco:check` fails the command (non-zero exit) and
prints the offending counter/ratio when coverage is under threshold;
`target/site/jacoco/index.html` (from the existing `report` execution)
shows the breakdown per package/class.

For a **scoped review**, override `jacoco.check.includes` (default `**/*`,
the whole project) to the package(s) the change touched, so the ratio is
computed only over those classes instead of the whole backend:

```
mvn jacoco:check -Djacoco.check.includes=com/jpablodrexler/photomanager/application/usecase/album/**
```

### 19.3 Flagging

🟡 Flag any coverage run — whole-project or scoped to the reviewed change —
that reports under 80% line coverage. This is a test-adequacy problem, not
a correctness bug, so it's a WARNING rather than CRITICAL, matching how
§18's complexity threshold is treated — but it should be fixed before the
review is considered clean: add the missing test cases for the uncovered
lines/branches the report lists (`java-unit-test-developer` for backend
gaps, `cypress-unit-test-developer` for frontend gaps), then re-run the
check to confirm it now clears 80%.

🟢 A scope in the 75–80% range is worth a passing mention if the gap is a
small, easily-covered handful of lines, but isn't required to be flagged —
the threshold that matters is 80%.

### 19.4 Trending snapshots

For a full-codebase sweep, both `npm run coverage:trend-report`
(`scripts/code-coverage-report.js`, frontend) and `bash
scripts/coverage-report.sh` (run from `backend/`, backend) wrap the same
suite runs the gates above use into a dated snapshot under
`docs/reports/code-coverage/`/`JPPhotoManagerWeb/docs/reports/code-coverage/`
— the project-wide percentages plus a table of every file/class still
below 80%. Unlike `coverage:check`/`jacoco:check`, these do not fail the
command; they exist purely to leave a written record of the actual number
over time, so a file that quietly backslid is visible even while the
project-wide number stays above threshold. The backend script parses
`target/site/jacoco/jacoco.xml` directly (via an inline Perl snippet, since
the file is single-line, deeply-nested XML that plain `grep`/`awk` cannot
reliably disambiguate between method/class/package/report-level counters
sharing the same tag name) rather than adding a second JaCoCo plugin
execution the way §18.4's backend complexity report needed — JaCoCo's
existing `report` execution (bound to the `test` phase) already produces
everything this needs.

---

## 20. Type Coverage (frontend)

Every reviewed frontend scope also gets a type-coverage pass — how much of
the TypeScript identifier surface has a real, non-`any` type, as opposed
to `any` reached via an explicit annotation, an untyped third-party return
value, or TypeScript inference giving up. This is the quantitative
backstop for §11's "no `any` unless unavoidable" convention: that rule
catches an `any` a reviewer happens to read past, this catches one that
slipped through review entirely.

Run from `frontend/`:

```
npx type-coverage --project tsconfig.app.json --detail
```

(or `npm run type-coverage:report` for a dated snapshot under
`docs/reports/type-coverage/`, listing every uncovered identifier grouped
by file — better for a full-codebase sweep than a scoped review).

🟡 Flag any identifier the tool reports as untyped that isn't already
caught by §11's manual `any` check. Same severity as that rule, not a
separate threshold-based gate — a single untyped identifier is exactly as
fixable regardless of the project-wide percentage.

🟢 Don't chase the last fraction of a percent in files that are
overwhelmingly typed already — note the project-wide percentage but only
flag specific uncovered identifiers actually touched by the reviewed
change (or, for a full sweep, the files with the most per the report's
file-grouped table).

---

## 21. Dead Code

Every full-codebase sweep also gets an unused-code pass — the automated
counterpart to whatever "reuse/simplification" findings a manual read
would catch, just extended to catch what a single-file read cannot: an
export or dependency nothing references *anywhere else* in the codebase.

### 21.1 Frontend (knip)

Run from `frontend/`:

```
npx knip
```

(or `npm run dead-code:report` for a dated snapshot under
`docs/reports/dead-code/`, split into unused files/exports/types/
dependencies/unlisted-dependencies). Configured in `frontend/knip.jsonc` —
see that file's comments for why Cypress config/spec files and a handful
of name-resolved devDependencies (`@angular-devkit/build-angular`,
`babel-plugin-istanbul`) need explicit entries/ignores before the tool's
findings are trustworthy on this Angular + Cypress project.

### 21.2 Backend (Maven)

Run from `backend/`:

```
mvn dependency:analyze
```

(or `bash scripts/dead-code-report.sh` for a dated snapshot under
`JPPhotoManagerWeb/docs/reports/dead-code/`). Maven's own built-in
unused/undeclared-dependency detector — the closest backend equivalent to
knip, though narrower in scope: it only covers dependencies, not unused
application-source exports/classes, since Maven has no direct analog to
knip's source-level dead-code detection.

**Known, expected noise:** every `spring-boot-starter-*` "umbrella"
dependency is reported as "unused declared" because `dependency:analyze`
works by scanning compiled bytecode for direct class references, and a
starter POM has no classes of its own — it exists purely to pull in a
bundle of real dependencies transitively. This is a well-documented
limitation of bytecode-based analysis for Spring Boot specifically, not a
real finding — skim past every `spring-boot-starter-*` entry and focus on
anything else in the "Unused declared dependencies" list.

### 21.3 Flagging

🟡 Flag an unused file, export, or type (frontend) — either it should be
made module-private or, if nothing in the codebase needs it anymore,
deleted outright per this project's own "no half-finished implementations"
convention.

🟡 Flag an unused declared dependency (either sub-project, excluding
§21.2's Spring Boot starter noise) — dead weight in the build and a wider
(if unused) attack surface for `security-reviewer` §1 to worry about.

🟢 Flag an `unlisted` dependency (frontend: imported but only present
transitively; backend: `dependency:analyze`'s "Used undeclared
dependencies") as a suggestion — it works today only because some other
direct dependency happens to pull it in, fragile across dependency-tree
changes.

---

## 22. Performance & Accessibility (Lighthouse, frontend)

Scoped narrowly today: `npm run lighthouse:report` (self-builds the
production bundle and audits it — see
`frontend/scripts/lighthouse-report.mjs`) only covers `/login`, the one
route reachable without an authenticated session (every other route is
behind `authGuard`, and there is no self-registration flow to also cover —
accounts are admin-created via `/admin/users`). There is no equivalent to
the mocked E2E tier's session-fabrication trick for Lighthouse, so this
does not run against any authenticated route yet.

Run from `frontend/`:

```
npm run lighthouse:report
```

Writes a dated snapshot to `docs/reports/lighthouse/` — Performance,
Accessibility, Best Practices, and SEO scores (0–100), plus every failed
accessibility audit by name.

🟡 Flag any accessibility audit failure on a route touched by the reviewed
change — Angular Material does not guarantee WCAG compliance for free,
and this is the only automated a11y signal this project has.

🟢 A performance/SEO score regression is worth a passing mention (note the
before/after numbers if both are available) but isn't a hard gate the way
§18/§19's thresholds are — there is no established baseline yet for either
score.

---

## 23. Deep Accessibility Audit (cypress-axe, frontend)

§22's Lighthouse accessibility score only ever covers `/login` — the one
route reachable without an authenticated session — and even there it's a
single aggregate number, not the actual rule that failed. `npm run
a11y:report` (`frontend/scripts/a11y-report.js`) is the deep, per-route
complement: it drives `cypress-axe` (axe-core through Cypress) against every
authenticated route (`/home`, `/gallery`, `/sync`, `/convert`, `/duplicates`,
`/admin/users`, `/albums`, `/albums/:id`, `/recycle-bin`, `/analytics`,
`/profile/sessions`) using the same session-fabrication trick as the mocked
E2E smoke tier (`visitWithSession()` from
`cypress/support/mocked/seed-session.ts`, `cy.intercept`-stubbed `/api/**`
calls — no live backend needed).

Run from `frontend/` (needs the dev server reachable at
`http://localhost:4200` first, e.g. `npm start` in another terminal):

```
npm run a11y:report
```

The underlying spec (`cypress/e2e/a11y/a11y-audit.cy.ts`, driven by its own
`cypress.a11y.config.ts`) lives outside both existing Cypress tiers —
excluded from `cypress.config.ts`'s `e2e.excludeSpecPattern` and never
matched by `cypress.mocked.config.ts`'s narrower `specPattern` — so it never
runs as part of `npm run test:e2e` or `npm run test:e2e:mocked`. It calls
`cy.checkA11y(..., skipFailures: true)`, so a real violation is recorded as a
finding, not a failed test; the report script also tolerates a non-zero
Cypress exit code rather than crashing.

Writes a dated snapshot to `docs/reports/a11y/` — total violations by
impact level (critical/serious/moderate/minor), then a per-route breakdown
of every violated WCAG rule (rule id, impact, affected selector(s), help
URL).

🔴 Flag any `critical` or `serious` impact violation on a route touched by
the reviewed change.

🟡 Flag any `moderate` impact violation on a route touched by the reviewed
change.

🟢 A `minor` impact violation, or any violation on a route the change didn't
touch, is worth a passing mention but isn't required to be flagged.

---

## 24. Mutation Testing (both sub-projects)

Line coverage (§19) only proves a line *executed* during the test suite —
it says nothing about whether a test actually *asserts* on that line's
behavior. Mutation testing closes that gap: a tool systematically changes
("mutates") small pieces of the code under test — flips a `>` to `>=`,
negates a condition, swaps a boolean literal — reruns the tests, and checks
whether anything failed. A "killed" mutant means some test caught the
change; a "survived" mutant means the whole suite still passed with the
code's behavior altered, i.e. that line has execution coverage but no real
assertion behind it. Unlike §18/§19/§21/§22, this is not wired as a
pass/fail gate anywhere (no CI job, no `npm run *:check` equivalent) — it's
a trending snapshot only, run on demand, the same way §18.4's complexity
trend report and the dead-code/dependency-staleness reports are.

### 24.1 Frontend (StrykerJS)

Run from `frontend/`:

```
npm run mutation:report
```

(`scripts/mutation-report.js`, driven by `stryker.conf.mjs`). Cypress
Component Testing has no native Stryker test-runner plugin (only
Jest/Mocha/Karma/Jasmine/Vitest are supported that way), so this uses
StrykerJS's generic **command** runner — and the config carries two
non-obvious things worth knowing before touching it:

- **The browser/`process` bridge.** The command runner's mutant-switch
  instrumentation reads `process.env.__STRYKER_ACTIVE_MUTANT__` to decide
  which mutant is "active," but the code under test runs inside a Cypress
  *browser* iframe, which has no Node `process` global. Without a bridge,
  every mutant silently "survives" — the switch never activates, the
  original code always runs, and the mutation score is permanently stuck
  at 0% regardless of test quality (confirmed empirically while building
  this: an identical mutation applied by hand and run directly correctly
  failed its test, while the same mutation via `npx stryker run` did not).
  The fix lives in `cypress.config.ts`'s component `setupNodeEvents`
  (forwards the env var into `config.env`) and
  `cypress/support/component.ts` (reads it via `Cypress.env(...)` and
  assigns `globalThis.process` before any spec file's own imports
  evaluate — ES module evaluation order guarantees the support file's
  top-level code runs first). If a mutation score is ever suspiciously and
  uniformly ~0% again, check that bridge before assuming the tests
  themselves are weak.
- **Narrow, curated scope.** `stryker.conf.mjs`'s `mutate` list is a
  hand-picked ~8 files under `core/` with genuine branching logic
  (services/interceptors with real conditionals — not thin HTTP CRUD
  wrappers), and `commandRunner.command` runs a scoped
  `cypress run --component --spec` (`npm run test:mutation`) covering only
  those files' own specs, not the full ~650-test suite. The command runner
  reruns the whole configured command per mutant — Stryker's live
  "remaining time" estimate is unreliable in the first few percent (it
  initially extrapolates from the slow first mutant, which pays a
  one-time Electron/npx warm-up cost the rest don't), so don't judge
  whether a run is worth letting finish from an early ETA: the full
  8-file scope (553 mutants) completed in under 14 minutes end to end at
  `concurrency: 1`, despite briefly showing an in-run estimate north of 8
  hours. When reviewing a scope this narrow, treat the resulting score as
  a sample of core/'s branchiest logic, not a whole-frontend figure —
  extend `mutate` to a changed file directly (temporarily, for a scoped
  review) rather than trusting the existing list to already cover it.

Writes a dated snapshot to `docs/reports/mutation/` — overall mutation
score, killed/survived/timeout/no-coverage counts, a per-file table sorted
worst-first, and a full list of surviving/uncovered mutants (file, line,
mutator, replacement).

### 24.2 Backend (PIT)

Run from `backend/`:

```
bash scripts/mutation-report.sh
```

Invokes `org.pitest:pitest-maven` (`backend/pom.xml`) via
`mvn org.pitest:pitest-maven:mutationCoverage@mutation-report` — a
report-only execution bound to `phase>none<`, mirroring
`maven-pmd-plugin`'s `complexity-report` execution (§18.2/§18.4), so it
never runs during `mvn test`/`verify`/`package`. Unlike the frontend, PIT
runs entirely inside the JVM test process alongside the real JUnit
suite — no browser boundary to bridge, and far faster than the frontend's
per-mutant-process command-runner approach for a comparable mutant count
(the full `application.usecase` scope — 65 classes, 443 mutants — runs in
about 4 minutes). `targetClasses`/`targetTests` are scoped to
`com.jpablodrexler.photomanager.application.usecase.*` — the actual
business-logic layer; `infrastructure.web`/`infrastructure.persistence`
are thin translation code not worth mutating.

Writes a dated snapshot to `docs/reports/mutation/` — PIT's own Line
Coverage / Mutation Coverage / Test Strength percentages, killed/survived/
no-coverage/timed-out counts, a per-class table sorted worst-first, and a
full list of surviving/uncovered mutants (class, method, line, mutator).

### 24.3 Flagging

🟡 Flag a survived mutant on a line the reviewed change touched — the test
suite runs and passes even though that line's behavior changed, meaning
whatever test covers it isn't actually asserting on the behavior a real
bug there would break. Fix by strengthening the existing test's
assertions (not by adding a redundant new test) so it would fail against
the mutant's changed behavior; re-run the relevant report to confirm.

🟡 Flag a "no coverage" mutant on a line the reviewed change touched — no
test reaches that line at all, a stronger gap than a survived mutant (that
line has neither execution coverage nor an assertion), and often a
`java-unit-test-developer`/`cypress-unit-test-developer` gap worth cross-
referencing against §19's coverage report for the same file.

🟢 Don't chase every survived mutant project-wide in a scoped review —
both reports' scope is already narrow (frontend: a curated ~8-file list;
backend: one architectural layer), so treat every surviving mutant inside
that scope as worth a look, but don't expand the mutation-testing scope
itself as part of an unrelated review.

---

## 25. Secrets Scanning, License Compliance, and Dependency Vulnerabilities (frontend)

Three more report-only metrics, all frontend-scoped tooling but scanning
beyond `frontend/` where relevant:

**Secrets scanning** — `npm run secrets:report`
(`frontend/scripts/secrets-scan-report.js`) scans the whole repository —
backend Java source, k8s manifests, docs, everything — not just
`frontend/` — with [secretlint](https://github.com/secretlint/secretlint)
for accidentally committed API keys, private keys, tokens, and other
high-confidence secret patterns. Uses secretlint's `unix` output formatter
deliberately, not `json` — the json formatter dumps every scanned file's
full unmasked content alongside any findings, both slow and a real
exposure risk if that output is ever mishandled. Every credential-shaped
file in this repo (`JPPhotoManagerWeb/.env`, `JPPhotoManagerWeb/k8s/secret.yaml`,
`JPPhotoManagerWeb/k8s/catalog-volumes.yaml`) is excluded via
`.secretlintignore` at the repo root, on top of secretlint's own
`.gitignore` respect. Writes a dated snapshot to
`docs/reports/secrets-scan/` — file, line, rule, and message per finding.

**License compliance** — `npm run license:report`
(`frontend/scripts/license-compliance-report.js`, via
[license-checker-rseidelsohn](https://github.com/RSeidelsohn/license-checker-rseidelsohn))
checks every resolved npm dependency's declared license, flagging the
copyleft family (GPL/AGPL/LGPL/SSPL/EUPL/CC-BY-SA/OSL/CPAL) and anything
unresolvable — this repo is public, so an unnoticed copyleft dependency is
a real concern. The backend equivalent is `bash backend/scripts/license-report.sh`
(license-maven-plugin's `add-third-party` goal, invoked ad hoc via its full
plugin coordinate — no `pom.xml` changes needed). Writes a dated snapshot
to `docs/reports/license-compliance/`.

**Dependency vulnerabilities (SCA)** — `npm run sca:report`
(`frontend/scripts/sca-report.js`, via `npm audit`) checks every resolved
npm dependency against npm's own advisory database. The backend equivalent
is `bash backend/scripts/sca-report.sh`, which queries
[OSV.dev](https://osv.dev)'s public batch API against the resolved Maven
dependency tree — chosen over OWASP Dependency-Check specifically because
Dependency-Check's first run downloads the full NVD database, rate-limited
to a multi-hour pull without a personally-requested API key. Both write a
dated snapshot to `docs/reports/dependency-vulnerabilities/`.

🔴 Any secrets-scan finding that's a real, live secret is a **stop what
you're doing** situation — rotate/revoke it immediately (git history still
has it even after removal from the working tree).

🔴 Flag any **critical/high** SCA finding with a fix available and no clear
reason it hasn't been applied.

🟡 Flag a copyleft or unknown license on a **runtime** dependency (Maven
compile scope / npm `dependencies`, not devDependencies or test scope) —
most flagged packages will be build/test tooling never distributed with
the app, which doesn't need the same scrutiny.

🟡 Flag a critical/high SCA finding with no fix available yet — worth
checking whether the vulnerable code path is actually reachable (many
advisories are in build tooling, never shipped or executed against
untrusted input) before treating it as urgent.

🟢 A secrets-scan false positive, an `UNKNOWN` license, or a moderate/low
SCA finding is worth a passing mention, not a blocker.
