## 1. Domain — Use case interface and DTO

- [ ] 1.1 Create `domain/port/in/folder/GetFolderStatsUseCase.java` with `FolderStats execute(String folderPath)`
- [ ] 1.2 Create `domain/model/FolderStats.java` record with `long assetCount`, `long totalSizeBytes`

## 2. Application — Use case implementation

- [ ] 2.1 Create `application/usecase/folder/GetFolderStatsUseCaseImpl.java` annotated with `@Service`
- [ ] 2.2 Inject `AssetRepositoryPort`; query count and sum of `fileSize` where `folderPath = :path` and `deleted = false`

## 3. HTTP adapter

- [ ] 3.1 Add `GET /api/folders/stats` (with `@RequestParam String path`) to `FolderController`
- [ ] 3.2 Return `FolderStatsResponse { long assetCount, long totalSizeBytes }`

## 4. Backend unit tests

- [ ] 4.1 Test that `GetFolderStatsUseCaseImpl` returns correct count and size for a folder with assets
- [ ] 4.2 Test that soft-deleted assets are excluded from the count
- [ ] 4.3 Test that an empty folder returns `{ assetCount: 0, totalSizeBytes: 0 }`

## 5. Frontend — FolderNavComponent

- [ ] 5.1 Add `getFolderStats(path: string): Observable<FolderStats>` to `FolderService`
- [ ] 5.2 Add `FolderStats` interface to `core/models/folder-stats.model.ts`: `{ assetCount: number, totalSizeBytes: number }`
- [ ] 5.3 In `FolderNavComponent`, maintain a `Map<string, FolderStats>` for cached stats
- [ ] 5.4 On tree node expand, call `getFolderStats(node.path)` if not already cached
- [ ] 5.5 Display stats inline in the node label: `{{ node.name }} ({{ stats.assetCount }} · {{ stats.totalSizeBytes | fileSize }})`

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
