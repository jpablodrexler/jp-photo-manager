## 1. Database migration

- [ ] 1.1 Create `V20__create_folder_bookmarks.sql`: `CREATE TABLE folder_bookmarks (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), folder_path VARCHAR(1024) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE (user_id, folder_path))`

## 2. Domain — Use case interfaces and model

- [ ] 2.1 Create `domain/model/FolderBookmark.java` record: `long id`, `String folderPath`, `Instant createdAt`
- [ ] 2.2 Create `domain/port/in/folder/GetFolderBookmarksUseCase.java` returning `List<FolderBookmark>`
- [ ] 2.3 Create `domain/port/in/folder/AddFolderBookmarkUseCase.java` returning `FolderBookmark`
- [ ] 2.4 Create `domain/port/in/folder/RemoveFolderBookmarkUseCase.java`

## 3. Application — Use case implementations

- [ ] 3.1 `GetFolderBookmarksUseCaseImpl`: query `folder_bookmarks` by `userId` sorted by `createdAt ASC`
- [ ] 3.2 `AddFolderBookmarkUseCaseImpl`: check count ≤ 10 (throw `ValidationException` if exceeded); insert or return existing (handle `DataIntegrityViolationException` → return existing row)
- [ ] 3.3 `RemoveFolderBookmarkUseCaseImpl`: find by `id` and `userId`; throw `ResourceNotFoundException` if not found; delete

## 4. HTTP adapter

- [ ] 4.1 Add `GET /api/folders/bookmarks` to `FolderController`
- [ ] 4.2 Add `POST /api/folders/bookmarks` with request body `{ "folderPath": "..." }`
- [ ] 4.3 Add `DELETE /api/folders/bookmarks/{id}` returning `204 No Content`

## 5. Backend unit tests

- [ ] 5.1 Test that `AddFolderBookmarkUseCaseImpl` returns 400 when 10 bookmarks already exist
- [ ] 5.2 Test that bookmarking an existing path returns the existing bookmark (idempotent)
- [ ] 5.3 Test that `RemoveFolderBookmarkUseCaseImpl` throws `ResourceNotFoundException` for wrong user
- [ ] 5.4 Test that `GET /api/folders/bookmarks` returns only the authenticated user's bookmarks

## 6. Frontend — FolderNavComponent

- [ ] 6.1 Add `getBookmarks(): Observable<FolderBookmark[]>`, `addBookmark(folderPath: string): Observable<FolderBookmark>`, and `removeBookmark(id: number): Observable<void>` to `FolderService`
- [ ] 6.2 Load bookmarks on `ngOnInit`; maintain a `bookmarkedPaths: Set<string>` for O(1) lookup
- [ ] 6.3 Render pinned section above tree: `@if (bookmarks.length > 0)` → `<mat-divider>` separator + `@for (b of bookmarks)` → clickable folder row
- [ ] 6.4 Add pin `MatIconButton` to each tree node: use `bookmark` icon if bookmarked, `bookmark_border` if not; toggle on click

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
