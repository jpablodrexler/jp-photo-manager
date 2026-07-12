---
name: java-developer
description: >
  Java developer skill for the JPPhotoManager Spring Boot 3.4 / Java 21
  backend. TRIGGER whenever work touches JPPhotoManagerWeb/backend — including
  when implementing OpenSpec tasks: adding a new use case, controller, entity,
  repository, DTO, or enum; fixing a bug in any Java class; refactoring or
  extracting logic into a new class; wiring up transactions, async operations,
  or Spring beans. Enforces hexagonal (ports and adapters) architecture
  (infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service),
  the port-interface / adapter-implementation split for every service and
  repository, and all other coding standards. Invoke this skill proactively —
  do not wait to be asked.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Java Developer Skill

Write Java code that follows the conventions and best practices of the
JPPhotoManager backend project: a Spring Boot 3.4 / Java 21 application with
hexagonal (ports and adapters) architecture, Lombok, MapStruct, Spring Data
JPA, Flyway, and PostgreSQL.

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

`com.jpablodrexler.photomanager.*`

---

## 2. Package / Layer Structure

```
src/main/java/com/jpablodrexler/photomanager/
  domain/
    model/              → Pure POJO domain objects (no framework imports, no JPA)
    port/
      in/               → Use-case interfaces (one interface, one method each)
        asset/          → GetAssetsUseCase, DeleteAssetsUseCase, …
        catalog/        → CatalogAssetsUseCase, GetDuplicatedAssetsUseCase, …
        folder/         → GetSubFoldersUseCase, GetDrivesUseCase, …
        sync/           → SyncAssetsUseCase, GetSyncConfigUseCase, …
        convert/        → ConvertAssetsUseCase, GetConvertConfigUseCase, …
        recycle/        → GetDeletedAssetsUseCase, RestoreAssetsUseCase, …
        home/           → GetHomeStatsUseCase
        user/           → ListUsersUseCase, CreateUserUseCase, …
      out/              → Repository and service port interfaces (driven ports)
    enums/              → ImageRotation, SortCriteria, ReasonEnum, FileType, …

  application/
    dto/                → Framework-free application DTOs
                          (CatalogChangeNotification, PaginatedResult, AssetFilter, …)
    usecase/            → One @Service implementation per use-case interface
      asset/
      catalog/
      folder/
      sync/
      convert/
      recycle/
      home/
      user/

  infrastructure/
    persistence/
      entity/           → @Entity JPA classes (NOT in domain/)
      jpa/              → Spring Data JPA interfaces (JpaXxxRepository extends JpaRepository)
      adapter/          → Implements domain/port/out/ repository interfaces (XxxRepositoryImpl)
      mapper/           → MapStruct @Mapper(componentModel="spring") entity ↔ domain model
    web/
      controller/       → @RestController classes (HTTP primary adapters)
      dto/              → HTTP request/response DTOs, split by direction:
        request/        → {BaseName}RequestDto — incoming @RequestBody/@RequestParam payloads only
        response/       → {BaseName}ResponseDto — outgoing ResponseEntity<...>/return-type payloads only
        shared/         → DTOs genuinely used as both request AND response payloads (rare —
                          verify every usage site before placing a class here instead of
                          request/ or response/); keep the existing name, no forced suffix
      mapper/           → MapStruct @Mapper(componentModel="spring") domain ↔ HTTP DTO
      exception/        → GlobalExceptionHandler and HTTP exception types
    service/            → Service adapters implementing domain/port/out/ service ports
                          (StorageServiceAdapter, ThumbnailStorageServiceAdapter, …)
    batch/              → Spring Batch job config, readers, processors, writers, partitioners
    config/             → AppConfig, SecurityConfig, UserConfig, DataInitializer

src/main/resources/
  application.yml
  db/migration/             # Flyway SQL files: V{n}__{Description}.sql

src/test/java/
  # Unit tests: mirror the main package structure
  # Integration tests: @SpringBootTest + @ActiveProfiles("test")

src/test/resources/
  application-test.yml      # PostgreSQL dialect, Flyway enabled (datasource from Testcontainers)
```

**Dependency flow:**
`infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service`

Domain must not import from any other layer. Use-cases in `application/usecase/`
may only import from `domain/`. Infrastructure adapters may import from `domain/`
but never from `application/usecase/` or `infrastructure/web/`.

---

## 3. Port / Adapter Split

The hexagonal architecture enforces three distinct port/adapter pairs:

### 3.1 Use-Case Ports (driving ports)

Declare a **single-method interface** in `domain/port/in/<subpackage>/`.
Implement it in `application/usecase/<subpackage>/` with `@Service @Transactional`.

| File | Package | Role |
|------|---------|------|
| `FooUseCase.java` | `domain/port/in/<subpackage>/` | Interface — one method |
| `FooUseCaseImpl.java` | `application/usecase/<subpackage>/` | `@Service implements FooUseCase` |

```java
// domain/port/in/catalog/CatalogAssetsUseCase.java
public interface CatalogAssetsUseCase {
    CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener);
}

// application/usecase/catalog/CatalogAssetsUseCaseImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {
    // injects only domain/port/out/ interfaces
    private final FolderRepository folderRepository;
    private final StoragePort storagePort;

    @Override
    @Transactional
    public CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener) { ... }
}
```

### 3.2 Service Ports (driven ports)

Declare the interface in `domain/port/out/` with a `Port` suffix.
Implement it in `infrastructure/service/` with a `ServiceAdapter` suffix.

| File | Package | Role |
|------|---------|------|
| `StoragePort.java` | `domain/port/out/` | Interface |
| `StorageServiceAdapter.java` | `infrastructure/service/` | `@Service implements StoragePort` |

### 3.3 Repository Ports (driven ports)

Declare the interface in `domain/port/out/` with a `Repository` suffix.
Implement it in `infrastructure/persistence/adapter/` with a `RepositoryImpl` suffix.
The impl delegates to a Spring Data JPA interface in `infrastructure/persistence/jpa/`.

| File | Package | Role |
|------|---------|------|
| `AssetRepository.java` | `domain/port/out/` | Interface |
| `AssetRepositoryImpl.java` | `infrastructure/persistence/adapter/` | `@Repository implements AssetRepository` |
| `JpaAssetRepository.java` | `infrastructure/persistence/jpa/` | `extends JpaRepository<AssetEntity, Long>` |

**Callers always inject the domain port interface, never the adapter class.**

---

## 4. Naming Conventions

| Element            | Convention                                             | Example                                         |
| ------------------ | ------------------------------------------------------ | ----------------------------------------------- |
| Classes            | PascalCase                                             | `AssetController`, `CatalogAssetsUseCaseImpl`   |
| Methods            | camelCase                                              | `getAssets()`, `execute()`                      |
| Fields             | camelCase                                              | `folderRepository`, `storagePort`               |
| Constants          | UPPER_SNAKE_CASE                                       | `THUMBNAIL_MAX_WIDTH`, `PAGE_SIZE`              |
| Enum values        | UPPER_SNAKE_CASE                                       | `ASSET_CREATED`, `FILE_NAME`                    |
| Use-case interface | `FooUseCase` in `domain/port/in/<pkg>/`                | `CatalogAssetsUseCase`                          |
| Use-case impl      | `FooUseCaseImpl` in `application/usecase/<pkg>/`       | `CatalogAssetsUseCaseImpl`                      |
| Service port       | `FooPort` in `domain/port/out/`                        | `StoragePort`, `ThumbnailPort`                  |
| Service adapter    | `FooServiceAdapter` in `infrastructure/service/`       | `StorageServiceAdapter`                         |
| Repository port    | `FooRepository` in `domain/port/out/`                  | `AssetRepository`, `FolderRepository`           |
| Repository adapter | `FooRepositoryImpl` in `infrastructure/persistence/adapter/` | `AssetRepositoryImpl`                     |
| JPA repository     | `JpaFooRepository` in `infrastructure/persistence/jpa/` | `JpaAssetRepository`                           |
| JPA entity         | `FooEntity` in `infrastructure/persistence/entity/`    | `AssetEntity`, `FolderEntity`                   |
| Domain model       | Plain class in `domain/model/`                         | `Asset`, `Folder`                               |
| HTTP request DTO   | `{BaseName}RequestDto` in `infrastructure/web/dto/request/` | `CreateAlbumRequestDto`, `RateAssetRequestDto` |
| HTTP response DTO  | `{BaseName}ResponseDto` in `infrastructure/web/dto/response/` | `AssetResponseDto`, `AlbumSummaryResponseDto` |
| HTTP shared DTO    | Unchanged name in `infrastructure/web/dto/shared/`     | `UserPreferenceDto` (used as both request and response body) |
| Test classes       | `{ClassName}Test` or `{ClassName}Tests`                | `CatalogAssetsUseCaseImplTest`                  |
| Test methods       | `methodName_condition_expectedResult`                  | `execute_folderExists_returnsAssets`            |
| DB migration files | `V{n}__{Description}.sql`                              | `V1__initial_schema.sql`                        |

---

## 5. Annotations & Lombok

### Lombok

```java
@Data                    // domain models, DTOs — generates getters, setters, equals, hashCode, toString
@Builder                 // domain models — fluent construction in tests
@NoArgsConstructor       // JPA entities (required by Hibernate)
@AllArgsConstructor      // JPA entities (used with @Builder)
@RequiredArgsConstructor // services, adapters — enables constructor injection
@Slf4j                   // inject logger: log.info(), log.error(), log.debug()
```

### Spring (Web layer)

```java
@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
@GetMapping / @PostMapping / @PutMapping / @PatchMapping / @DeleteMapping
@RequestParam / @PathVariable / @RequestBody
@Valid                   // trigger bean validation on @RequestBody
```

### OpenAPI (web layer only)

Every `@RestController` must carry springdoc-openapi annotations.
`springdoc-openapi-starter-webmvc-ui` is on the classpath; Swagger UI is
served at `/swagger-ui.html`.

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Assets", description = "Photo and video asset management")
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    @Operation(summary = "List assets in a folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated asset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> getAssets(...) { ... }
}
```

Rules:
- `@Tag` — one per controller class; `name` is the Swagger group label.
- `@Operation(summary = "...")` — one per endpoint method; one short sentence.
- `@ApiResponses` — list every HTTP status code the method can return.
- Do **not** add these annotations to domain interfaces, use cases, or infrastructure adapters.

### Spring (Application / Infrastructure)

```java
@Service
@Repository             // on persistence adapters in infrastructure/persistence/adapter/
@Async                  // long-running operations; return CompletableFuture<T>
@Transactional          // write operations on use-case methods
@Transactional(readOnly = true)  // read-only use-case methods
```

### Spring (Configuration)

```java
@Configuration
@Bean
@Value("${property.name:default}")
```

### JPA / Persistence (infrastructure/persistence/entity/ only)

```java
@Entity
@Table(name = "assets")
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "asset_id")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "folder_id", nullable = false)
@Enumerated(EnumType.STRING)
```

JPA annotations belong **only** on `*Entity` classes in `infrastructure/persistence/entity/`.
Domain model classes in `domain/model/` must be pure POJOs — no JPA imports.

### Validation (HTTP DTOs only)

```java
@NotBlank
@NotEmpty
@Valid
```

---

## 6. Layer-by-Layer Patterns

### 6.1 Web Layer — Controllers & HTTP DTOs

- **Thin controllers:** inject use-case interfaces and delegate immediately.
- Return `ResponseEntity<T>` with correct HTTP status codes (200, 201, 204, 404, 500).
- Use MapStruct mappers in `infrastructure/web/mapper/` for HTTP DTO ↔ domain model conversions.
- Use `SseEmitter` for streaming progress of long-running operations.
- **DTO placement:** every class in `infrastructure/web/dto/` lives in exactly one of
  `request/`, `response/`, or `shared/` — never directly in `web/dto/`. Classify by usage,
  not by guessing from the name: a class is `request/` only if it is exclusively an incoming
  `@RequestBody`/`@RequestParam` payload; `response/` only if it is exclusively an outgoing
  `ResponseEntity<...>`/return-type payload (including when nested inside another response
  DTO); `shared/` only if the *exact same class* is verified to appear as both, across every
  controller method that references it.

```java
@Tag(name = "Assets", description = "Photo and video asset management")
@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final GetAssetsUseCase getAssetsUseCase;
    private final AssetWebMapper assetWebMapper;   // MapStruct

    @Operation(summary = "List assets in a folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated asset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedResult<AssetResponseDto>> getAssets(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int page) {
        PaginatedResult<Asset> result = getAssetsUseCase.execute(folderPath, page);
        return ResponseEntity.ok(assetWebMapper.toDto(result));
    }
}
```

**HTTP response DTO pattern** (in `infrastructure/web/dto/response/`):

```java
@Data
public class AssetResponseDto {
    private Long assetId;
    private String fileName;
    private String thumbnailUrl;
}
```

**HTTP request DTO pattern** (in `infrastructure/web/dto/request/`):

```java
public record RateAssetRequestDto(@Min(0) @Max(5) int rating) {}
```

### 6.2 Application Layer — Use Cases

- One implementation class per use-case interface.
- `@Service @Transactional` owns the transaction boundary.
- Injects only `domain/port/out/` interfaces — no JPA, no Spring MVC.
- Does not contain business logic — orchestrates ports.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAssetsUseCaseImpl implements GetAssetsUseCase {

    private final AssetRepository assetRepository;   // domain port

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> execute(String folderPath, int page) {
        return assetRepository.findFiltered(new AssetFilter(folderPath, page));
    }
}
```

### 6.3 Domain Layer — Models & Port Interfaces

Domain models are **pure POJOs** — no Spring, no JPA, no Lombok `@Builder` dependencies on other layers.

```java
// domain/model/Asset.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private Long assetId;
    private Folder folder;
    private String fileName;
    private long fileSize;
    private String hash;
    private FileType fileType;

    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
```

Use-case port interface (one method, no default implementations):

```java
// domain/port/in/asset/GetAssetsUseCase.java
public interface GetAssetsUseCase {
    PaginatedResult<Asset> execute(String folderPath, int page);
}
```

Repository port interface (domain contract for persistence):

```java
// domain/port/out/AssetRepository.java
public interface AssetRepository {
    Optional<Asset> findById(Long id);
    PaginatedResult<Asset> findFiltered(AssetFilter filter);
    List<Asset> findByFolder(Folder folder);
    Asset save(Asset asset);
    void deleteById(Long id);
}
```

### 6.4 Infrastructure — Persistence Adapter

The persistence adapter implements the domain repository port.
It delegates to the Spring Data JPA interface and uses a MapStruct mapper.

```java
// infrastructure/persistence/adapter/AssetRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {

    private final JpaAssetRepository jpaRepository;
    private final AssetMapper mapper;           // MapStruct

    @Override
    public Optional<Asset> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Asset save(Asset asset) {
        AssetEntity entity = mapper.toEntity(asset);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
```

### 6.5 Infrastructure — MapStruct Mappers

All entity ↔ domain model and HTTP DTO ↔ domain model conversions use MapStruct.
Hand-writing mappers is not permitted.

```java
// infrastructure/persistence/mapper/AssetMapper.java
@Mapper(componentModel = "spring")
public interface AssetMapper {
    Asset toDomain(AssetEntity entity);
    AssetEntity toEntity(Asset domain);
}

// infrastructure/web/mapper/AssetWebMapper.java
@Mapper(componentModel = "spring")
public interface AssetWebMapper {
    AssetResponseDto toDto(Asset domain);
    PaginatedResult<AssetResponseDto> toDto(PaginatedResult<Asset> result);
}
```

Use `@Named` qualifiers when a mapper exposes multiple methods returning the same type
(e.g., `toEntityRef` for FK-only references vs `toEntity` for full mapping).

### 6.6 JPA Delete-Then-Insert Pattern

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

### 6.7 Spring Proxy & `@Transactional` — self-invocation pitfall

Spring applies `@Transactional` (and `@Async`) via a proxy. When a method calls
**another method on the same bean** (`this.foo()`), it bypasses the proxy entirely —
so `@Transactional` on the called method **has no effect**.

**Wrong — `@Transactional` is silently ignored:**

```java
@Service
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    @Async
    public CompletableFuture<Void> execute(...) {
        catalogFolder(...);          // self-invocation: proxy bypassed
        return CompletableFuture.completedFuture(null);
    }

    @Transactional                   // never fires — called from same bean
    protected void catalogFolder(...) { ... }
}
```

**Right — extract the transactional work to a separate bean:**

```java
@Service
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final CatalogFolderPort catalogFolderPort; // separate bean

    @Async
    public CompletableFuture<Void> execute(...) {
        catalogFolderPort.catalogFolder(...); // proxy intercepts → @Transactional fires
        return CompletableFuture.completedFuture(null);
    }
}

@Service
public class CatalogFolderServiceAdapter implements CatalogFolderPort {

    @Transactional                   // fires correctly — called from a different bean
    public void catalogFolder(...) {
        // private helpers run in the same transaction
    }
}
```

---

## 7. Async & Streaming

- Annotate long-running methods with `@Async`.
- Return `CompletableFuture<T>` so the caller is non-blocking.
- Accept a `Consumer<T>` callback for progress streaming.
- Stream progress to the client using `SseEmitter` in the controller.
- Configure the thread pool in `AppConfig`:

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

## 8. Error Handling & Logging

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

## 9. Pagination

- Accept `page` (0-based) and an optional `SortCriteria` enum from callers via `AssetFilter`.
- Return a generic `PaginatedResult<T>` wrapper from use cases.

---

## 10. Configuration

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
    com.jpablodrexler: INFO
```

**Local development prerequisite:** PostgreSQL 18+ must be running:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:18
```

### application-local.yml (local developer override)

Override machine-specific properties without committing them. Add to `application.yml`:

```yaml
spring:
  config:
    import: optional:classpath:application-local.yml
```

Create `src/main/resources/application-local.yml` (gitignored):

```yaml
photomanager:
  initial-directory: ${user.home}/Imágenes
  root-catalog-folders: ${user.home}/Imágenes
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

No JDBC URL — Testcontainers injects it via `@ServiceConnection`. Integration tests extend `PostgresIntegrationTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
}
```

### Inject properties

```java
@Value("${photomanager.catalog-batch-size:1000}")
private int catalogBatchSize;
```

---

## 11. Database Migrations (Flyway)

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
- Date/time columns: `TIMESTAMP` (Hibernate maps `LocalDateTime` natively)
- Do **not** use `IF NOT EXISTS` on `CREATE TABLE` — Flyway manages idempotency

---

## 12. Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock StoragePort storagePort;
    @Mock AssetRepository assetRepository;
    @InjectMocks CatalogAssetsUseCaseImpl sut;

    @Test
    void execute_filesFound_createsAssets() throws Exception {
        when(storagePort.listFiles(any())).thenReturn(List.of("/photos/a.jpg"));

        CompletableFuture<Void> future = sut.execute(notification -> {});
        future.get();

        verify(assetRepository, times(1)).save(any(Asset.class));
    }
}
```

### Integration Tests

Extend `PostgresIntegrationTest` — it starts a PostgreSQL container automatically via Testcontainers:

```java
class CatalogIntegrationTest extends PostgresIntegrationTest {

    @Autowired CatalogAssetsUseCase catalogAssetsUseCase;

    @Test
    void contextLoads() {
        assertThat(catalogAssetsUseCase).isNotNull();
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

## 13. Java 21 Features

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

## 14. Code Style Rules

- **New use cases:** declare a single-method interface in `domain/port/in/<subpackage>/`, implement in `application/usecase/<subpackage>/` with `@Service @Transactional`. Inject only `domain/port/out/` interfaces.
- **New service ports:** declare interface (suffix `Port`) in `domain/port/out/`, implement (suffix `ServiceAdapter`) in `infrastructure/service/`.
- **New repository ports:** declare interface (suffix `Repository`) in `domain/port/out/`, implement (suffix `RepositoryImpl`) in `infrastructure/persistence/adapter/`, backed by a Spring Data JPA interface (prefix `Jpa`) in `infrastructure/persistence/jpa/`.
- **New controllers:** add to `infrastructure/web/controller/`; inject use-case interfaces only; use MapStruct mappers for all HTTP DTO ↔ domain model conversion.
- **New HTTP DTOs:** place in `infrastructure/web/dto/request/`, `infrastructure/web/dto/response/`, or `infrastructure/web/dto/shared/` based on verified usage — never directly in `infrastructure/web/dto/`. Name request DTOs `{BaseName}RequestDto` and response DTOs `{BaseName}ResponseDto`; `shared/` DTOs keep their existing name (no forced suffix) since they don't have a single direction.
- **MapStruct only** — never hand-write mappers between layers.
- **JPA entities** belong only in `infrastructure/persistence/entity/`; domain models in `domain/model/` must be plain POJOs.
- Never call a `@Transactional` or `@Async` method from within the same bean (`this.foo()`) — use a separate injected bean so the Spring proxy can intercept the call.
- No `System.out.println` — use `@Slf4j`.
- No field injection (`@Autowired` on fields) — use constructor injection via `@RequiredArgsConstructor`.
- No `open-in-view` (keep it `false`) — load associations within transactions.
- Use `@Transactional(readOnly = true)` for read-only use-case methods.
- Use `FetchType.LAZY` for all `@ManyToOne` and `@OneToMany` relationships on JPA entities.
- Validation annotations (`@NotBlank`, `@NotEmpty`) belong **only** on HTTP DTOs in `infrastructure/web/dto/request/` (or `shared/`), not on entities or domain models.
- Keep controllers thin — one method per endpoint, immediate delegation to the use case.
- Prefer streams and method references over imperative loops.
- Add comments only when the **why** is non-obvious; omit them otherwise.

---

## 15. Spring Security

### 15.1 JWT with HttpOnly Cookies

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

### 15.2 SseEmitter + Spring Security Async Dispatch

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

### 15.3 SecurityConfig Circular Dependency

Extract `UserDetailsService` and `PasswordEncoder` beans into a separate `@Configuration` class (`UserConfig`) to break the cycle between `SecurityConfig` and `JwtAuthenticationFilter`.

### 15.4 CORS: Include All HTTP Methods in Use

```java
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
```

Always keep this list in sync with the HTTP methods actually used by the API.

---

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new database migration files added
3. How to run the tests for the new code:
   ```bash
   cd JPPhotoManagerWeb/backend && mvn test -Dtest=YourNewTest
   ```
4. Any configuration properties the user should set
