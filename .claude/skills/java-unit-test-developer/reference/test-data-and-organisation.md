# Java Unit Test Developer — Test Data Setup & Test Organisation Rules

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

