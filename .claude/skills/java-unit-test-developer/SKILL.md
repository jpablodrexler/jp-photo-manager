---
name: java-unit-test-developer
description: >
  JUnit 5 unit and integration test skill for the JPPhotoManager Spring Boot
  3.4 / Java 21 backend. Use when creating, fixing, or updating test files for
  any backend class: services, repositories, controllers, facades, or entities.
  Enforces the project's testing conventions: Mockito for mocking,
  AssertJ for assertions, sut naming, one concept per test, and the
  method_condition_result naming pattern.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Java Unit Test Developer Skill

Write JUnit 5 tests that follow the conventions and best practices of the
JPPhotoManager backend project: Spring Boot 3.4 / Java 21, Mockito, AssertJ,
and clean architecture layering.

## Workflow

Make a todo list and work through it one task at a time.

---

## 1. Project Setup

### Test dependencies (managed by Spring Boot parent POM)

`spring-boot-starter-test` pulls in all required test dependencies automatically:

| Library     | Version (managed) | Role                              |
| ----------- | ----------------- | --------------------------------- |
| JUnit 5     | 5.x               | Test runner and annotations       |
| Mockito     | 5.x               | Mocking and stubbing              |
| AssertJ     | 3.x               | Fluent assertions                 |
| Spring Test | managed           | `@SpringBootTest`, `MockMvc`, etc |
| Hamcrest    | 2.x               | (available but AssertJ preferred) |

No additional `pom.xml` entries are needed for unit tests; `spring-boot-starter-test` is already declared with `<scope>test</scope>`.

### Test source tree

```
src/test/java/com/jpablodrexler/photomanager/
  infrastructure/service/   # unit tests for service implementations
  application/              # unit tests for PhotoManagerFacade
  api/                      # unit tests for controllers (optional — prefer integration)
  domain/                   # unit tests for domain logic / entity helpers

src/test/resources/
  application-test.yml      # in-memory SQLite, Flyway disabled
```

Mirror the main package structure exactly. A test for
`infrastructure/service/CatalogFolderServiceImpl.java` lives at
`infrastructure/service/CatalogFolderServiceImplTest.java`.

---

## 2. File Naming and Location

| Rule                                                                 | Example                                                             |
| -------------------------------------------------------------------- | ------------------------------------------------------------------- |
| Test class sits in the same package as the class under test          | `CatalogFolderServiceImplTest` alongside `CatalogFolderServiceImpl` |
| File name is `{ClassName}Test.java` or `{ClassName}Tests.java`       | `CatalogAssetsServiceImplTest.java`                                 |
| Integration test classes use the `IT` suffix or `Integration` suffix | `ApplicationIntegrationTest.java`                                   |

---

## 3. Naming Conventions

| Element       | Convention                             | Example                                      |
| ------------- | -------------------------------------- | -------------------------------------------- |
| Test class    | `{ClassName}Test`                      | `CatalogAssetsServiceImplTest`               |
| Test method   | `methodName_condition_expectedResult`  | `catalogFolder_newFile_createsAsset`         |
| SUT variable  | always `sut`                           | `@InjectMocks CatalogFolderServiceImpl sut;` |
| Mock variable | `{type}` or `{field}` name (camelCase) | `@Mock StorageService storageService;`       |

---

## 4. Unit Test Structure

Every unit test class follows this structure:

```java
@ExtendWith(MockitoExtension.class)
class CatalogFolderServiceImplTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock StorageService storageService;
    @Mock ThumbnailStorageService thumbnailStorageService;
    @InjectMocks CatalogFolderServiceImpl sut;

    @Test
    void catalogFolder_newFolder_savesFolderToRepository() {
        String folderPath = "/photos";
        when(folderRepository.findByPath(folderPath)).thenReturn(Optional.empty());
        when(storageService.listImageFiles(folderPath)).thenReturn(List.of());
        when(assetRepository.findByFolder(any())).thenReturn(List.of());
        when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.catalogFolder(folderPath, notification -> {}, new AtomicInteger(0), 1);

        verify(folderRepository).save(argThat(f -> f.getPath().equals(folderPath)));
    }

    @Test
    void catalogFolder_existingAsset_doesNotCreateDuplicate() {
        Folder folder = new Folder();
        folder.setPath("/photos");
        Asset existing = new Asset();
        existing.setFileName("a.jpg");

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listImageFiles("/photos")).thenReturn(List.of("a.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(existing));

        sut.catalogFolder("/photos", notification -> {}, new AtomicInteger(0), 1);

        verify(assetRepository, never()).save(any());
    }
}
```

**Rules:**

- Use `@ExtendWith(MockitoExtension.class)` — **never** `@SpringBootTest` in unit tests.
- Declare mocks with `@Mock`; inject them with `@InjectMocks`. Never call `new` on the sut.
- Name the system under test `sut` (always).
- Use AssertJ `assertThat(...)` for all assertions; never use `assertEquals` / `assertTrue`.
- One concept per test method — a test that asserts two unrelated things must be split.
- No shared mutable state between test methods (no mutable `@BeforeEach` state that bleeds into other tests via side effects).

---

## 5. Mockito Patterns

### Stubbing return values

```java
when(storageService.directoryExists("/photos")).thenReturn(true);
when(folderRepository.findByPath(any())).thenReturn(Optional.of(folder));
when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
```

### Stubbing void methods

```java
doNothing().when(thumbnailStorageService).saveThumbnail(anyLong(), any(byte[].class));
doThrow(new RuntimeException("disk full")).when(storageService).writeFile(any(), any());
```

### Verification

```java
verify(assetRepository, times(1)).save(any(Asset.class));
verify(folderRepository, never()).delete(any());
verify(storageService, atLeastOnce()).listImageFiles(any());
```

### ArgumentCaptor — inspect what was passed to a mock

```java
ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
verify(assetRepository).save(captor.capture());
Asset saved = captor.getValue();
assertThat(saved.getFileName()).isEqualTo("photo.jpg");
assertThat(saved.getFolder()).isEqualTo(folder);
```

### argThat — inline predicate matching

```java
verify(folderRepository).save(argThat(f -> f.getPath().equals("/photos")));
```

### Throwing exceptions from stubs

```java
when(storageService.listImageFiles(any())).thenThrow(new IOException("disk error"));
```

---

## 6. AssertJ Assertions Reference

Always import `org.assertj.core.api.Assertions.assertThat`:

| Scenario                   | AssertJ assertion                                                             |
| -------------------------- | ----------------------------------------------------------------------------- |
| Not null                   | `assertThat(result).isNotNull()`                                              |
| Equality                   | `assertThat(result).isEqualTo(expected)`                                      |
| Boolean true/false         | `assertThat(flag).isTrue()` / `.isFalse()`                                    |
| List size                  | `assertThat(list).hasSize(3)`                                                 |
| List contains element      | `assertThat(list).contains(element)`                                          |
| List contains exactly      | `assertThat(list).containsExactly(a, b, c)`                                   |
| List is empty              | `assertThat(list).isEmpty()`                                                  |
| String contains            | `assertThat(str).contains("substring")`                                       |
| String starts with         | `assertThat(str).startsWith("prefix")`                                        |
| Optional is present        | `assertThat(opt).isPresent()`                                                 |
| Optional has value         | `assertThat(opt).hasValue(expected)`                                          |
| Exception thrown           | `assertThatThrownBy(() -> sut.method()).isInstanceOf(RuntimeException.class)` |
| Exception message          | `assertThatThrownBy(...).hasMessageContaining("disk error")`                  |
| Field value via extracting | `assertThat(asset).extracting(Asset::getFileName).isEqualTo("photo.jpg")`     |

---

## 7. Testing `@Async` Methods

`@Async` is only active when Spring manages the bean. In a unit test with
`@ExtendWith(MockitoExtension.class)` there is no Spring context — the method
runs synchronously. Call `.get()` on the returned `CompletableFuture` to wait
for completion and propagate any exceptions:

```java
@Test
void catalogAssetsAsync_validDirectory_processesAllFolders() throws Exception {
    when(storageService.directoryExists(any())).thenReturn(true);
    when(storageService.listSubDirectories(any())).thenReturn(List.of());

    CompletableFuture<Void> future = sut.catalogAssetsAsync(notification -> {});
    future.get(); // blocks until completion; re-throws ExecutionException on failure

    verify(catalogFolderService, atLeastOnce()).catalogFolder(any(), any(), any(), anyInt());
}
```

If you need to assert on the callback notifications collected during the async run:

```java
@Test
void catalogAssetsAsync_newAsset_sendsCreatedNotification() throws Exception {
    List<CatalogChangeNotification> notifications = new ArrayList<>();
    when(storageService.directoryExists(any())).thenReturn(true);
    when(storageService.listSubDirectories(any())).thenReturn(List.of());

    sut.catalogAssetsAsync(notifications::add).get();

    // verify notification contents if the service produces them directly
}
```

---

## 8. Testing `@Transactional` Methods

`@Transactional` is a Spring proxy concern — it has **no effect** in a unit test
with `@ExtendWith(MockitoExtension.class)`. Unit tests verify _logic_, not
transaction boundaries. To verify that `@Transactional` works end-to-end write
an integration test (see §10).

In unit tests, simply call the method directly and verify mock interactions:

```java
@Test
void catalogFolder_ioError_logsErrorAndContinues() {
    when(folderRepository.findByPath(any())).thenReturn(Optional.empty());
    when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(storageService.listImageFiles(any())).thenThrow(new RuntimeException("I/O error"));

    // should not throw — error is caught and logged
    assertThatCode(() ->
        sut.catalogFolder("/photos", notification -> {}, new AtomicInteger(0), 1)
    ).doesNotThrowAnyException();
}
```

---

## 9. Testing Facade Methods

The facade (`PhotoManagerFacade`) delegates to domain services and repositories.
Unit test it by mocking its dependencies:

```java
@ExtendWith(MockitoExtension.class)
class PhotoManagerFacadeTest {

    @Mock AssetRepository assetRepository;
    @Mock CatalogAssetsService catalogAssetsService;
    @InjectMocks PhotoManagerFacade sut;

    @Test
    void getAssets_folderHasAssets_returnsPaginatedResult() {
        Folder folder = new Folder();
        folder.setPath("/photos");
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        asset.setFolder(folder);

        Page<Asset> page = new PageImpl<>(List.of(asset));
        when(assetRepository.findByFolderPath(eq("/photos"), any(Pageable.class))).thenReturn(page);

        PaginatedData<Asset> result = sut.getAssets("/photos", 0, SortCriteria.FILE_NAME);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalItems()).isEqualTo(1);
        assertThat(result.getPageIndex()).isZero();
    }
}
```

---

## 10. Integration Tests

Integration tests load the full Spring context against an in-memory SQLite database.
Flyway migrations are disabled; the schema is created by `ddl-auto: create-drop`.

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

### Structure

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CatalogFolderServiceIntegrationTest {

    @Autowired CatalogFolderService sut;
    @Autowired FolderRepository folderRepository;
    @Autowired AssetRepository assetRepository;

    @AfterEach
    void cleanup() {
        assetRepository.deleteAll();
        folderRepository.deleteAll();
    }

    @Test
    void catalogFolder_newFolder_persistsFolderAndAsset() {
        // use a temp directory with a real image file if needed,
        // or mock StorageService via @MockitoBean
        // ...
        assertThat(folderRepository.findAll()).hasSize(1);
    }
}
```

**Rules:**

- Always annotate integration tests with `@ActiveProfiles("test")` — without it the test
  will target the production SQLite database.
- Use `@MockitoBean` to replace Spring beans that touch the real filesystem (e.g. `StorageService`).
- Clean up persistent state in `@AfterEach` to keep tests independent.
- Use `WebEnvironment.NONE` unless the test exercises HTTP endpoints.

---

## 11. Testing Controllers (Slice Tests)

Use `@WebMvcTest` to test a single controller in isolation without starting the
full application context:

```java
@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PhotoManagerFacade facade;

    @Test
    void getAssets_validFolderPath_returns200() throws Exception {
        PaginatedData<Asset> page = new PaginatedData<>();
        page.setItems(List.of());
        page.setPageIndex(0);
        page.setTotalPages(0);
        page.setTotalItems(0);

        when(facade.getAssets(eq("/photos"), eq(0), any())).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                .param("folderPath", "/photos")
                .param("pageIndex", "0"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.items").isArray())
               .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void deleteAsset_unknownId_returns404() throws Exception {
        when(facade.deleteAsset(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/assets/99"))
               .andExpect(status().isNotFound());
    }
}
```

**Rules:**

- `@WebMvcTest` auto-configures `MockMvc` and only loads the specified controller.
- Use `@MockitoBean` for every bean the controller depends on (the facade and any others).
- Use `jsonPath(...)` from `spring-test` for JSON response assertions.
- Never use `@SpringBootTest` just to test a controller.

---

## 12. Test Data Setup

Keep test data inline and minimal — only the fields the test actually exercises:

```java
private Asset buildAsset(String fileName, Folder folder) {
    Asset asset = new Asset();
    asset.setFileName(fileName);
    asset.setFolder(folder);
    asset.setImageRotation(ImageRotation.ROTATE_0);
    return asset;
}

private Folder buildFolder(String path) {
    Folder folder = new Folder();
    folder.setPath(path);
    return folder;
}
```

For integration tests that need persisted data, save via the repository in `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    Folder folder = folderRepository.save(buildFolder("/photos"));
    assetRepository.save(buildAsset("photo.jpg", folder));
}
```

---

## 13. Test Organisation Rules

- **One `@Test` method per concept** — do not mix multiple assertions that test different
  behaviours in a single test method.
- **No shared mutable state** between test methods. Each test must be able to run in any order.
- **`@BeforeEach` for repeated setup** — extract common mock stubs that every test in the class
  needs; leave test-specific stubs inside the test method itself.
- **Descriptive method names** using the `methodName_condition_expectedResult` pattern so failures
  self-document without needing to read the body.
- **Only `@ExtendWith(MockitoExtension.class)` for unit tests** — never `@SpringBootTest`,
  `@DataJpaTest`, or `@WebMvcTest` in unit test classes.
- **Only `@SpringBootTest` / `@WebMvcTest` for integration tests** — always paired with
  `@ActiveProfiles("test")`.

---

## 14. Running Tests

```bash
# All tests
cd JPPhotoManagerWeb/backend
mvn test

# Single test class
mvn test -Dtest=CatalogFolderServiceImplTest

# Single test method
mvn test -Dtest=CatalogFolderServiceImplTest#catalogFolder_newFolder_savesFolderToRepository

# All tests in a package
mvn test -Dtest="com.jpablodrexler.photomanager.infrastructure.service.*"

# Skip tests during build
mvn clean package -DskipTests
```

---

## Wrap Up

After creating or modifying test files, provide a brief summary covering:

1. Test files created or modified and the class or method they cover
2. Test type (unit with Mockito or integration with Spring context)
3. How to run the new tests:
   ```bash
   cd JPPhotoManagerWeb/backend
   mvn test -Dtest=YourNewTest
   ```
4. Any `@MockitoBean` or `application-test.yml` changes needed for integration tests
