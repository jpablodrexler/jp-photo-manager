# Java Developer — Layer-by-Layer Patterns — Infrastructure (Persistence, Mappers, Transactions)

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

Each entity/DTO pair gets **one mapper interface** — named `{Entity}Mapper`
(persistence) or `{Entity}WebMapper` (HTTP) — with a `toDto`/`toModel` (or `toDomain`)
method per direction. Never split one entity's conversions across multiple mapper
classes, and never hand-write a mapper method MapStruct could generate. Inject the
mapper into whatever needs it (controller, use case, repository adapter) via the
consumer's own Lombok `@RequiredArgsConstructor`, exactly like any other collaborator:

```java
@RestController
@RequiredArgsConstructor
public class AssetController {
    private final AssetWebMapper assetWebMapper;   // MapStruct interface, Lombok-injected
    ...
}
```

**If a conversion step needs a Spring bean the mapper can't reach directly** (e.g.
parsing a JSON column with the shared `ObjectMapper` instead of `new ObjectMapper()`
per call), do **not** turn the mapper into an `abstract class` with a Lombok
`@RequiredArgsConstructor`/field to inject it — MapStruct's generated `*Impl`
subclass does not reliably forward a Lombok-generated constructor and can still emit
a no-arg `super()` call, failing to compile even with the `lombok-mapstruct-binding`
annotation processor added (confirmed on this codebase). Instead, extract that one
conversion step into its own small `@Component` with a normal Lombok
`@RequiredArgsConstructor`, and wire it into the mapper with `uses = ...` +
`injectionStrategy = InjectionStrategy.CONSTRUCTOR`:

```java
@Component
@RequiredArgsConstructor
class AlbumFilterJsonDeserializer {
    private final ObjectMapper objectMapper;

    @Named("deserializeFilterJson")
    AlbumFilterJson deserializeFilterJson(String filterJson) { ... }
}

@Mapper(componentModel = "spring", uses = AlbumFilterJsonDeserializer.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AlbumWebMapper {
    @Mapping(source = "filterJson", target = "filterJson", qualifiedByName = "deserializeFilterJson")
    AlbumSummaryResponseDto toSummaryDto(AlbumData data);
}
```

The mapper stays a plain `interface` with no constructor of its own; the `ObjectMapper`
dependency still flows through ordinary Lombok constructor injection — just on the
small delegate `@Component`, not on the mapper itself.

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

