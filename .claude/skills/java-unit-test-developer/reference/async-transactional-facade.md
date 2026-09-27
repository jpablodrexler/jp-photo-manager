# Java Unit Test Developer — Testing @Async, @Transactional & Facade Methods

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

