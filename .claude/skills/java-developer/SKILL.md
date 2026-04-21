---
name: java-developer
description: >
  Java developer skill for writing Spring Boot applications following the
  JPPhotoManager backend code style. Use when creating or modifying Java
  classes (controllers, services, repositories, entities, DTOs, tests) in a
  Spring Boot project with a clean-architecture layering of api → application
  → domain ← infrastructure.
---

# Java Developer Skill

Write Java code that follows the conventions and best practices of the
JPPhotoManager backend project: a Spring Boot 3.4 / Java 21 application with
clean architecture, Lombok, MapStruct, Spring Data JPA, Flyway, and SQLite.

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

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Persistence |
| `spring-boot-starter-validation` | Bean validation |
| `spring-boot-starter-actuator` | Health/metrics |
| `lombok` | Boilerplate reduction |
| `mapstruct` | DTO mapping |
| `sqlite-jdbc` + `hibernate-community-dialects` | SQLite support |
| `flyway-core` | DB migrations |
| `spring-boot-starter-test` | JUnit 5 + Mockito |

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
  application-test.yml      # In-memory SQLite, Flyway disabled
```

**Dependency flow:** `api` → `application` → `domain` ← `infrastructure`

---

## 3. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `AssetController`, `CatalogAssetsServiceImpl` |
| Methods | camelCase | `getAssets()`, `catalogAssetsAsync()` |
| Fields | camelCase | `facade`, `assetRepository` |
| Constants | UPPER_SNAKE_CASE | `THUMBNAIL_MAX_WIDTH`, `PAGE_SIZE` |
| Enum values | UPPER_SNAKE_CASE | `ASSET_CREATED`, `FILE_NAME` |
| Test classes | `{ClassName}Test` or `{ClassName}Tests` | `CatalogAssetsServiceImplTest` |
| Test methods | `methodName_condition_expectedResult` | `getAssets_folderExists_returnsPage` |
| DB migration files | `V{n}__{Description}.sql` | `V1__Create_assets_table.sql` |

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
    @Column(name = "asset_id")
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

    private final AssetRepository assetRepository;
    private final StorageService storageService;

    @Async
    @Override
    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        try {
            List<String> files = storageService.listFiles(rootPath);
            for (String file : files) {
                processFile(file, callback);
            }
        } catch (Exception e) {
            log.error("Failed to catalog assets", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    private void processFile(String filePath, Consumer<CatalogChangeNotification> callback) {
        try {
            // business logic
            callback.accept(new CatalogChangeNotification(filePath, ReasonEnum.ASSET_CREATED));
        } catch (Exception e) {
            log.error("Failed to catalog asset: {}", filePath, e);
        }
    }
}
```

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
    url: jdbc:sqlite:${user.home}/.photomanager/photomanager.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

photomanager:
  initial-directory: ${user.home}/Pictures
  catalog-batch-size: 1000

logging:
  level:
    com.<company>: INFO
```

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:sqlite:file::memory:?cache=shared
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
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
-- V1__Create_folders_table.sql
CREATE TABLE IF NOT EXISTS folders (
    folder_id INTEGER PRIMARY KEY AUTOINCREMENT,
    path      TEXT NOT NULL UNIQUE
);

-- V2__Create_assets_table.sql
CREATE TABLE IF NOT EXISTS assets (
    asset_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id      INTEGER NOT NULL REFERENCES folders(folder_id),
    file_name      TEXT NOT NULL,
    image_rotation TEXT,
    hash           TEXT
);
```

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

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationIntegrationTest {

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

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new database migration files added
3. How to run the tests for the new code:
   ```bash
   ./mvnw test -pl backend -Dtest=YourNewTest
   ```
4. Any configuration properties the user should set
