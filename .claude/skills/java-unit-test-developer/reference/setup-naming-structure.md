# Java Unit Test Developer — Project Setup, Naming Conventions & Unit Test Structure

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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
  PostgresIntegrationTest.java  # abstract base class for @SpringBootTest integration tests
  infrastructure/service/       # unit tests for service implementations
  application/                  # unit tests for PhotoManagerFacade
  api/                          # unit tests for controllers (optional — prefer integration)
  domain/                       # unit tests for domain logic / entity helpers

src/test/resources/
  application-test.yml      # PostgreSQL dialect, Flyway enabled; datasource URL injected by Testcontainers
```

Mirror the main package structure exactly. A test for
`infrastructure/service/CatalogFolderServiceAdapter.java` lives at
`infrastructure/service/CatalogFolderServiceAdapterTest.java`.

---

## 2. File Naming and Location

| Rule                                                                 | Example                                                             |
| -------------------------------------------------------------------- | ------------------------------------------------------------------- |
| Test class sits in the same package as the class under test          | `CatalogFolderServiceAdapterTest` alongside `CatalogFolderServiceAdapter` |
| File name is `{ClassName}Test.java` or `{ClassName}Tests.java`       | `CatalogAssetsServiceImplTest.java`                                 |
| Integration test classes use the `IT` suffix or `Integration` suffix | `ApplicationIntegrationTest.java`                                   |

---

## 3. Naming Conventions

| Element       | Convention                             | Example                                      |
| ------------- | -------------------------------------- | -------------------------------------------- |
| Test class    | `{ClassName}Test`                      | `CatalogAssetsServiceImplTest`               |
| Test method   | `methodName_condition_expectedResult`  | `catalogFolder_newFile_createsAsset`         |
| SUT variable  | always `sut`                           | `@InjectMocks CatalogFolderServiceAdapter sut;` |
| Mock variable | `{type}` or `{field}` name (camelCase) | `@Mock StorageService storageService;`       |

---

## 4. Unit Test Structure

Every unit test class follows this structure:

```java
@ExtendWith(MockitoExtension.class)
class CatalogFolderServiceAdapterTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock StorageService storageService;
    @Mock ThumbnailStorageService thumbnailStorageService;
    @InjectMocks CatalogFolderServiceAdapter sut;

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

