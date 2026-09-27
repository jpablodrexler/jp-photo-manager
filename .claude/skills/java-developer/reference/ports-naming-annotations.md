# Java Developer — Port/Adapter Split, Naming Conventions & Annotations/Lombok

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

### 3.4 Do not create a delegate-only port/adapter

Before adding a new `FooPort`/`FooServiceAdapter` pair, check whether the
capability already exists on another port (`StoragePort` in particular covers
a lot: file I/O, hashing, thumbnails, EXIF). If it does, inject that existing
port directly — do not create a new port whose adapter's only job is to call
the existing one (`FooAdapter.doThing() { return storagePort.doThing(); }`).
A port/adapter earns its existence by containing real logic; a pure
pass-through is dead weight and, worse, a second "source of truth" that can
silently drift from the original (this happened: `HashCalculatorPort`/
`AssetHashCalculatorAdapter` duplicated `StoragePort.computeHash`'s SHA-256
logic instead of reusing it, and neither was ever depended on for the actual
capability — see the `code-reviewer` skill §15 for the full incident). If you
inherit or find such a pair, delete it and repoint any callers to the port
that has the real implementation, rather than making it delegate.

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

