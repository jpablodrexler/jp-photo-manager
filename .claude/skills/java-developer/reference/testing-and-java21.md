# Java Developer — Testing & Java 21 Features

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

