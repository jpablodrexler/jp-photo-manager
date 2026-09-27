# Java Developer — Project Setup & Package/Layer Structure

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

