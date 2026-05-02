---
name: java-developer
description: >
  Java developer skill for the JPPhotoManager Spring Boot 3.4 / Java 21
  backend. TRIGGER whenever work touches JPPhotoManagerWeb/backend — including
  when implementing OpenSpec tasks: adding a new service, controller, entity,
  repository, DTO, or enum; fixing a bug in any Java class; refactoring or
  extracting logic into a new class; wiring up transactions, async operations,
  or Spring beans. Enforces clean-architecture layering
  (api → application → domain ← infrastructure), the
  interface-in-domain / implementation-in-infrastructure split for every
  service, and all other coding standards. Invoke this skill proactively —
  do not wait to be asked.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Java Developer Skill

Write Java code that follows the conventions and best practices of the
JPPhotoManager backend project: a Spring Boot 3.4 / Java 21 application with
clean architecture, Lombok, MapStruct, Spring Data JPA, Flyway, and PostgreSQL.

## Workflow

Make a todo list and work through it one task at a time.

---

## 1. Project Setup

### Build System

Use **Maven** with the Spring Boot parent POM:

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.4.4</version>
</parent>
```

**Java version:** 21

**Required dependencies:**

| Dependency                                     | Purpose               |
| ---------------------------------------------- | --------------------- |
| `spring-boot-starter-web`                      | REST API              |
| `spring-boot-starter-data-jpa`                 | Persistence           |
| `spring-boot-starter-validation`               | Bean validation       |
| `spring-boot-starter-actuator`                 | Health/metrics        |
| `lombok`                                       | Boilerplate reduction |
| `mapstruct`                                    | DTO mapping           |
| `postgresql`                                   | PostgreSQL JDBC driver |
| `flyway-core` + `flyway-database-postgresql`   | DB migrations         |
| `spring-boot-starter-test`                     | JUnit 5 + Mockito     |

### Package Root

`com.<company>.<appname>.*`

---

## 2. Package / Layer Structure

```
src/main/java/com/<company>/<app>/
  api/                      # REST controllers + request/response DTOs
    dto/
  application/              # Facade + application-level DTOs
    dto/
  config/                   # Spring @Configuration classes
  domain/
    entity/                 # JPA entities
    enums/                  # Domain enums
    repository/             # Spring Data JPA interfaces
    service/                # Service *interfaces* (domain contracts)
  infrastructure/
    service/                # Service *implementations*

src/main/resources/
  application.yml
  db/migration/             # Flyway SQL files: V{n}__{Description}.sql

src/test/java/
  # Unit tests: mirror the main package structure
  # Integration tests: @SpringBootTest + @ActiveProfiles("test")

src/test/resources/
  application-test.yml      # PostgreSQL dialect, Flyway enabled (datasource from Testcontainers)
```

**Dependency flow:** `api` → `application` → `domain` ← `infrastructure`

### Service interface / implementation rule

Every service **must** be split across two files:

| File                  | Package                   | Contents                                      |
| --------------------- | ------------------------- | --------------------------------------------- |
| `FooService.java`     | `domain/service/`         | Java interface — the domain contract          |
| `FooServiceImpl.java` | `infrastructure/service/` | `@Service` class that `implements FooService` |

Callers always depend on the **interface** (`FooService`), never the impl. This applies to every service without exception — even small internal helpers extracted to fix a Spring proxy issue.

```java
// domain/service/CatalogFolderService.java
public interface CatalogFolderService {
    void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                       AtomicInteger processed, int total);
    Asset createAsset(String directoryPath, String fileName);
}

// infrastructure/service/CatalogFolderServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogFolderServiceImpl implements CatalogFolderService {
    // ...
}
```

---

## 3. Naming Conventions

| Element            | Convention                              | Example                                       |
| ------------------ | --------------------------------------- | --------------------------------------------- |
| Classes            | PascalCase                              | `AssetController`, `CatalogAssetsServiceImpl` |
| Methods            | camelCase                               | `getAssets()`, `catalogAssetsAsync()`         |
| Fields             | camelCase                               | `facade`, `assetRepository`                   |
| Constants          | UPPER_SNAKE_CASE                        | `THUMBNAIL_MAX_WIDTH`, `PAGE_SIZE`            |
| Enum values        | UPPER_SNAKE_CASE                        | `ASSET_CREATED`, `FILE_NAME`                  |
| Test classes       | `{ClassName}Test` or `{ClassName}Tests` | `CatalogAssetsServiceImplTest`                |
| Test methods       | `methodName_condition_expectedResult`   | `getAssets_folderExists_returnsPage`          |
| DB migration files | `V{n}__{Description}.sql`               | `V1__Create_assets_table.sql`                 |

---

## 4. Annotations & Lombok

### Lombok

```java
@Data                   // entities, DTOs — generates getters, setters, equals, hashCode, toString
@NoArgsConstructor      // JPA entities (required by Hibernate)
@RequiredArgsConstructor // services — enables constructor injection
@Slf4j                  // inject logger: log.info(), log.error(), log.debug()
```

### Spring (API layer)

```java
@RestController
@RequestMapping("/api/v1/assets")
@CrossOrigin(origins = "*")
@GetMapping / @PostMapping / @PutMapping / @DeleteMapping
@RequestParam / @PathVariable / @RequestBody
@Valid                  // trigger bean validation on @RequestBody
```

### Spring (Service / Infrastructure)

```java
@Service
@Async                  // long-running operations; return CompletableFuture<T>
@Transactional          // write operations
@Transactional(readOnly = true)  // read-only queries
```

### Spring (Configuration)

```java
@Configuration
@Bean
@Value("${property.name:default}")
```

### JPA / Persistence

```java
@Entity
@Table(name = "assets")
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "asset_id")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "folder_id", nullable = false)
@Enumerated(EnumType.STRING)
@Transient              // computed/helper methods on entities
```

### Validation (API DTOs only)

```java
@NotBlank
@NotEmpty
@Valid
```

---

## 5. Layer-by-Layer Patterns

### 5.1 API Layer — Controllers & DTOs

- **Thin controllers:** delegate immediately to the application facade.
- Return `ResponseEntity<T>` with correct HTTP status codes (200, 201, 204, 404, 500).
- Map between API DTOs and domain objects in the controller (no business logic).
- Use `SseEmitter` for streaming progress of long-running operations.

```java
@RestController
@RequestMapping("/api/v1/assets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final PhotoManagerFacade facade;

    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> getAssets(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "FILE_NAME") SortCriteria sortCriteria) {
        PaginatedData<Asset> page = facade.getAssets(folderPath, pageIndex, sortCriteria);
        PaginatedData<AssetDto> result = page.map(AssetDto::from);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        boolean deleted = facade.deleteAsset(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
```

**API DTO pattern:**

```java
@Data
public class AssetDto {
    private Long assetId;
    private String fileName;
    private String thumbnailUrl;

    public static AssetDto from(Asset asset) {
        AssetDto dto = new AssetDto();
        dto.setAssetId(asset.getAssetId());
        dto.setFileName(asset.getFileName());
        return dto;
    }
}
```

### 5.2 Application Layer — Facade

- Orchestrates domain services and repositories.
- Owns `@Transactional` boundaries.
- Returns application DTOs (`PaginatedData<T>`, `SyncAssetsResult`, etc.).
- Does NOT contain business logic — delegates to domain services.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoManagerFacade {

    private final AssetRepository assetRepository;
    private final CatalogAssetsService catalogAssetsService;

    @Transactional(readOnly = true)
    public PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria) {
        Sort sort = buildSort(sortCriteria);
        PageRequest pageRequest = PageRequest.of(pageIndex, PAGE_SIZE, sort);
        Page<Asset> page = assetRepository.findByFolderPath(folderPath, pageRequest);
        return PaginatedData.from(page);
    }

    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        return catalogAssetsService.catalogAssetsAsync(callback);
    }
}
```

### 5.3 Domain Layer — Entities, Interfaces, Repositories

**Entity:**

```java
@Entity
@Table(name = "assets")
@Data
@NoArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")   // maps to BIGSERIAL in PostgreSQL
    private Long assetId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_rotation")
    private ImageRotation imageRotation;

    @Transient
    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
```

**Service interface (domain contract):**

```java
public interface CatalogAssetsService {
    CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback);
}
```

**Repository:**

```java
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByFolder(Folder folder);
    Page<Asset> findByFolderPath(String path, Pageable pageable);
    boolean existsByFolderAndFileName(Folder folder, String fileName);

    @Query("SELECT a.hash FROM Asset a GROUP BY a.hash HAVING COUNT(a) > 1")
    List<String> findDuplicateHashes();
}
```

### 5.4 Infrastructure Layer — Service Implementations

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    private final CatalogFolderService catalogFolderService; // injected by interface
    private final StorageService storageService;

    @Async
    @Override
    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        List<String> folders = discoverFolders();
        AtomicInteger processed = new AtomicInteger(0);
        for (String folderPath : folders) {
            try {
                // Calls through the Spring proxy → @Transactional on catalogFolder fires correctly
                catalogFolderService.catalogFolder(folderPath, callback, processed, folders.size());
            } catch (Exception e) {
                log.error("Error cataloging folder: {}", folderPath, e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
```

### 5.5 JPA Delete-Then-Insert Pattern

When replacing a complete set of JPA entities (e.g., saving new sync/convert configuration), use `deleteAllInBatch()` and set `id = null` on each incoming entity before `saveAll()`.

**Wrong — causes `ObjectOptimisticLockingFailureException`:**

```java
repository.deleteAll();               // incoming entities still carry old IDs
repository.saveAll(incomingEntities); // Hibernate tries to MERGE deleted rows → exception
```

**Correct:**

```java
repository.deleteAllInBatch();        // single SQL DELETE
for (int i = 0; i < incomingEntities.size(); i++) {
    incomingEntities.get(i).setId(null);   // forces INSERT, not MERGE
    incomingEntities.get(i).setOrder(i);   // preserve order if applicable
}
repository.saveAll(incomingEntities);
```

`deleteAllInBatch()` issues a single `DELETE` statement; `deleteAll()` loads then deletes each entity individually and leaves incoming entity IDs pointing at rows that no longer exist.

### 5.5 Spring Proxy & `@Transactional` — self-invocation pitfall

Spring applies `@Transactional` (and `@Async`) via a proxy that wraps the bean. When a method calls **another method on the same bean** (`this.foo()`), it bypasses the proxy entirely — so `@Transactional` on the called method **has no effect**.

**Wrong — `@Transactional` is silently ignored:**

```java
@Service
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    @Async
    public CompletableFuture<Void> catalogAssetsAsync(...) {
        catalogFolder(...);          // self-invocation: proxy bypassed
        return CompletableFuture.completedFuture(null);
    }

    @Transactional                   // never fires — called from same bean
    protected void catalogFolder(...) {
        createAsset(...);            // also self-invocation
    }

    @Transactional                   // never fires either
    public Asset createAsset(...) { ... }
}
```

**Right — extract the transactional work to a separate bean:**

```java
// The @Async bean calls the @Transactional bean through the proxy
@Service
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    private final CatalogFolderService catalogFolderService; // different bean → proxy active

    @Async
    public CompletableFuture<Void> catalogAssetsAsync(...) {
        catalogFolderService.catalogFolder(...); // goes through proxy → @Transactional fires
        return CompletableFuture.completedFuture(null);
    }
}

// All work that must share one transaction lives in its own service
@Service
public class CatalogFolderServiceImpl implements CatalogFolderService {

    @Transactional                   // fires correctly — called from a different bean
    public void catalogFolder(...) {
        Folder folder = folderRepository.findByPath(...).orElseGet(...); // MANAGED entity
        // private helpers called here run in the same transaction — no detached-entity issues
        createAssetInternal(folder, ...);
    }

    private Asset createAssetInternal(Folder folder, ...) { ... }
}
```

The consequence of getting this wrong: repository calls each run in their own micro-transaction, entities become detached between calls, and Hibernate may reject operations that reference those detached entities — causing silent failures where zero records are saved.

---

## 6. Async & Streaming

- Annotate long-running methods with `@Async`.
- Return `CompletableFuture<T>` so the caller is non-blocking.
- Accept a `Consumer<T>` callback for progress streaming.
- Stream progress to the client using `SseEmitter` in the controller.
- Configure the thread pool in `@Configuration`:

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("app-async-");
    executor.initialize();
    return executor;
}
```

---

## 7. Error Handling & Logging

- Inject the logger with `@Slf4j`; never use `System.out` or `System.err`.
- Log levels: `INFO` for normal flow, `ERROR` for failures, `DEBUG` for diagnostic traces.
- Always include context (file path, entity ID, etc.) in log messages.
- Catch specific exceptions; log with context; re-throw as `RuntimeException` or return a safe default.

```java
try {
    Asset asset = createAsset(folderPath, fileName);
    log.info("Cataloged asset: {}", asset.getFullPath());
} catch (IOException e) {
    log.error("Failed to read file: {}", filePath, e);
} catch (Exception e) {
    log.error("Unexpected error cataloging: {}", filePath, e);
    throw new RuntimeException("Catalog failed", e);
}
```

---

## 8. Pagination & Sorting

- Accept `pageIndex` (0-based) and a `SortCriteria` enum from callers.
- Use Spring Data `PageRequest.of(pageIndex, PAGE_SIZE, sort)`.
- Return a generic `PaginatedData<T>` wrapper:

```java
@Data
public class PaginatedData<T> {
    private List<T> items;
    private int pageIndex;
    private int totalPages;
    private long totalItems;

    public static <T> PaginatedData<T> from(Page<T> page) {
        PaginatedData<T> result = new PaginatedData<>();
        result.setItems(page.getContent());
        result.setPageIndex(page.getNumber());
        result.setTotalPages(page.getTotalPages());
        result.setTotalItems(page.getTotalElements());
        return result;
    }

    public <R> PaginatedData<R> map(java.util.function.Function<T, R> mapper) {
        PaginatedData<R> result = new PaginatedData<>();
        result.setItems(items.stream().map(mapper).collect(java.util.stream.Collectors.toList()));
        result.setPageIndex(pageIndex);
        result.setTotalPages(totalPages);
        result.setTotalItems(totalItems);
        return result;
    }
}
```

---

## 9. Configuration

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:photomanager}
    username: ${POSTGRES_USERNAME:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8080

photomanager:
  initial-directory: ${user.home}/Pictures
  catalog-batch-size: 1000

logging:
  level:
    com.<company>: INFO
```

**Local development prerequisite:** PostgreSQL 15+ must be running:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:15
```

### application-local.yml (local developer override)

To override properties for a specific developer's machine without committing them, add this to `application.yml`:

```yaml
spring:
  config:
    import: optional:classpath:application-local.yml
```

Then create `src/main/resources/application-local.yml` (gitignored) with only the properties that differ locally:

```yaml
photomanager:
  initial-directory: ${user.home}/Imágenes
  root-catalog-folders: ${user.home}/Imágenes
```

Add the file to `.gitignore` so it is never committed:

```
JPPhotoManagerWeb/backend/src/main/resources/application-local.yml
```

### application-test.yml

```yaml
spring:
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

No JDBC URL in the test config — Testcontainers injects the datasource URL at runtime via `@ServiceConnection`. Integration tests extend `PostgresIntegrationTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
}
```

### Inject properties

```java
@Value("${photomanager.catalog-batch-size:1000}")
private int catalogBatchSize;
```

---

## 10. Database Migrations (Flyway)

- Place SQL files in `src/main/resources/db/migration/`.
- Name them `V{n}__{Description}.sql` (two underscores).
- Never modify an applied migration; create a new one instead.

```sql
-- V1__initial_schema.sql  (PostgreSQL DDL)
CREATE TABLE folders (
    folder_id BIGSERIAL PRIMARY KEY,
    path      TEXT      NOT NULL
);

-- V2__add_example_table.sql
CREATE TABLE example (
    id          BIGSERIAL PRIMARY KEY,
    folder_id   BIGINT    NOT NULL REFERENCES folders(folder_id),
    name        TEXT      NOT NULL,
    active      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);
```

**Key PostgreSQL DDL conventions:**
- Auto-increment primary keys: `BIGSERIAL PRIMARY KEY`
- Boolean columns: `BOOLEAN NOT NULL DEFAULT FALSE` (never `INTEGER DEFAULT 0`)
- Date/time columns: `TIMESTAMP` (Hibernate maps `LocalDateTime` natively; no custom converter needed)
- Do **not** use `IF NOT EXISTS` on `CREATE TABLE` — Flyway manages idempotency

---

## 11. Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class CatalogAssetsServiceImplTest {

    @Mock StorageService storageService;
    @Mock AssetRepository assetRepository;
    @InjectMocks CatalogAssetsServiceImpl sut;

    @Test
    void catalogAssetsAsync_filesFound_createsAssets() throws Exception {
        when(storageService.listFiles(any())).thenReturn(List.of("/photos/a.jpg"));

        CompletableFuture<Void> future = sut.catalogAssetsAsync(notification -> {});
        future.get();

        verify(assetRepository, times(1)).save(any(Asset.class));
    }
}
```

### Integration Tests

Extend `PostgresIntegrationTest` — it starts a PostgreSQL container automatically via Testcontainers:

```java
class ApplicationIntegrationTest extends PostgresIntegrationTest {

    @Autowired PhotoManagerFacade facade;

    @Test
    void contextLoads() {
        assertThat(facade).isNotNull();
    }
}
```

### Key Rules

- Use `@ExtendWith(MockitoExtension.class)` for unit tests, not `@SpringBootTest`.
- Name the system under test `sut`.
- Use AssertJ (`assertThat(...)`) for all assertions.
- Use `@ActiveProfiles("test")` in integration tests to pick up `application-test.yml`.
- Keep tests isolated: no shared mutable state between test methods.
- One concept per test method.

---

## 12. Java 21 Features

Prefer modern Java 21 idioms where they simplify the code:

```java
// Records for simple, immutable DTOs
public record AssetImage(byte[] bytes, String fileName) {}

// Pattern matching in switch
String description = switch (imageRotation) {
    case ROTATE_90  -> "Landscape left";
    case ROTATE_270 -> "Landscape right";
    case ROTATE_180 -> "Upside down";
    default         -> "Normal";
};

// Pattern matching instanceof
if (result instanceof ErrorResult error) {
    log.error("Processing failed: {}", error.message());
}
```

---

## 13. Code Style Rules

- Every service **must** have an interface in `domain/service/` and an `*Impl` class in `infrastructure/service/`. Never create a `@Service` class without a corresponding interface.
- Never call a `@Transactional` or `@Async` method from within the same bean (`this.foo()`) — use a separate injected bean so the Spring proxy can intercept the call.
- No `System.out.println` — use `@Slf4j`.
- No field injection (`@Autowired` on fields) — use constructor injection via `@RequiredArgsConstructor`.
- No `open-in-view` (keep it `false`) — load associations within transactions.
- Use `@Transactional(readOnly = true)` for every read-only facade method.
- Use `FetchType.LAZY` for all `@ManyToOne` and `@OneToMany` relationships.
- Validation annotations (`@NotBlank`, `@NotEmpty`) belong **only** on API DTOs, not on entities.
- Keep controllers thin — one method per endpoint, immediate delegation to facade.
- Prefer streams and method references over imperative loops.
- Add comments only when the **why** is non-obvious; omit them otherwise.

---

## 14. Spring Security

### 14.1 JWT with HttpOnly Cookies

For browser-facing APIs, store the JWT in an **HttpOnly cookie** rather than requiring an `Authorization` header. Browsers send cookies automatically with all same-origin requests — including `<img src="...">` and `EventSource` — whereas custom headers are silently dropped by those APIs.

**Login — set the cookie:**

```java
ResponseCookie cookie = ResponseCookie.from("jwt", token)
        .httpOnly(true)
        .path("/")
        .sameSite("Strict")
        .maxAge(Duration.between(Instant.now(), expiresAt))
        .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

**Logout — clear the cookie:**

```java
ResponseCookie cookie = ResponseCookie.from("jwt", "")
        .httpOnly(true).path("/").sameSite("Strict").maxAge(0).build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

**Filter — read from cookie only:**

```java
private String resolveToken(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
            .filter(c -> "jwt".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst().orElse(null);
}
```

### 14.2 SseEmitter + Spring Security Async Dispatch

When `SseEmitter` writes events, Tomcat creates a secondary async dispatch thread. Spring Security's filter chain re-runs on that thread, but `SecurityContextHolder` is thread-local and empty → `AuthorizationDeniedException` with "response is already committed".

**Fix:** permit all `ASYNC` dispatcher types before the other rules:

```java
import jakarta.servlet.DispatcherType;

.authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()   // must be first
        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
)
```

This applies to every SSE endpoint (`/api/sync/run`, `/api/convert/run`, `/api/assets/catalog`).

### 14.3 SecurityConfig Circular Dependency

A cycle forms when `SecurityConfig` defines `@Bean UserDetailsService`, `SecurityConfig` also injects `JwtAuthenticationFilter`, and `JwtAuthenticationFilter` injects `UserDetailsService` — Spring cannot resolve the construction order.

**Fix:** extract `UserDetailsService` and `PasswordEncoder` beans into a separate `@Configuration` class:

```java
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class UserConfig {
    private final UserRepository userRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
            .map(u -> User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .roles("USER").build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

`SecurityConfig` then only injects `JwtAuthenticationFilter`; no cycle exists.

### 14.4 CORS: Include All HTTP Methods in Use

Browsers send an `Origin` header with state-changing requests even for same-origin SPA calls. If `PATCH` (or any other method) is missing from the CORS allowlist, preflight requests return 403.

```java
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
```

Always keep this list in sync with the set of HTTP methods actually used by the API.

---

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new database migration files added
3. How to run the tests for the new code:
   ```bash
   ./mvnw test -pl backend -Dtest=YourNewTest
   ```
4. Any configuration properties the user should set
