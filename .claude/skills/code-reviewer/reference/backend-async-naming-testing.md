# Code Reviewer — Backend: Async & SSE, Naming, Testing

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

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
