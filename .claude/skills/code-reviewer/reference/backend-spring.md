# Code Reviewer — Backend: Spring, Transactions, JPA & Migrations

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

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
