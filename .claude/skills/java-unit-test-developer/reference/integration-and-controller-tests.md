# Java Unit Test Developer — Integration Tests & Testing Controllers (Slice Tests)

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 10. Integration Tests

Integration tests load the full Spring context against a real PostgreSQL database managed by Testcontainers. Flyway migrations are enabled; the schema is applied from `V1__initial_schema.sql` before tests run. **Docker must be running.**

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

No JDBC URL is declared here — Testcontainers injects the datasource URL at runtime via `@ServiceConnection`.

### Shared base class

All `@SpringBootTest` integration tests must extend `PostgresIntegrationTest`:

```java
// src/test/java/com/jpablodrexler/photomanager/PostgresIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
}
```

### Structure

```java
class CatalogFolderServiceIntegrationTest extends PostgresIntegrationTest {

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

- Always extend `PostgresIntegrationTest` — it provides the container, the `@ActiveProfiles("test")`, and `@Testcontainers`.
- Use `@MockitoBean` to replace Spring beans that touch the real filesystem (e.g. `StorageService`).
- Clean up persistent state in `@AfterEach` to keep tests independent.
- Use `WebEnvironment.NONE` unless the test exercises HTTP endpoints.
- Do **not** use `@WebMvcTest` or `@ExtendWith(MockitoExtension.class)` for integration tests.

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

