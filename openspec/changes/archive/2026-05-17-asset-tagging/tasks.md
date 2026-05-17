> **Prerequisite:** Apply the `hexagonal-architecture` change before this one. The tasks below target the post-refactor package structure: use-case interfaces in `domain/port/in/`, driven ports in `domain/port/out/`, use-case implementations in `application/usecase/`, controllers in `infrastructure/web/controller/`, HTTP DTOs in `infrastructure/web/dto/`, JPA entities in `infrastructure/persistence/entity/`.

## 1. Backend: Database Schema

- [x] 1.1 Write Flyway migration `V<next>__create_tags_tables.sql` creating the `tags` table (`tag_id SERIAL PK`, `name VARCHAR(100) NOT NULL UNIQUE`) and the `asset_tags` join table (`asset_id BIGINT FK`, `tag_id BIGINT FK`, `PK(asset_id, tag_id)`)
- [x] 1.2 Create `TagEntity` JPA entity in `infrastructure/persistence/entity/` with `@ManyToMany` relationship to `AssetEntity`; create matching plain POJO `Tag` in `domain/model/` with a `String name` field
- [x] 1.3 Add `Set<TagEntity> tags` field to `AssetEntity` in `infrastructure/persistence/entity/` with `@ManyToMany(fetch = LAZY)` and `@JoinTable`; add `Set<String> tags` field to the `Asset` domain model in `domain/model/`; update `AssetEntityMapper` to map between the two representations

## 2. Backend: Repository Ports and Use Cases

- [x] 2.1 Create `domain/port/out/TagRepository.java` — plain Java interface with methods: `Optional<Tag> findByName(String name)`, `List<Tag> findByNameContaining(String q, int limit)`, `Tag save(Tag tag)`, `void deleteById(Long id)`, `boolean isUsedByOtherAssets(Long tagId, Long excludeAssetId)`; create `infrastructure/persistence/jpa/JpaTagRepository.java` extending `JpaRepository<TagEntity, Long>`; create `infrastructure/persistence/adapter/TagRepositoryImpl.java` implementing `TagRepository`
- [x] 2.2 Create use-case interfaces in `domain/port/in/tag/`: `AddTagToAssetUseCase.java` — `void execute(Long assetId, String name)`; `RemoveTagFromAssetUseCase.java` — `void execute(Long assetId, String name)`; `BulkAddTagUseCase.java` — `void execute(List<Long> assetIds, String name)`; `BulkRemoveTagUseCase.java` — `void execute(List<Long> assetIds, String name)`; `ListTagsUseCase.java` — `List<Tag> execute(String query)`
- [x] 2.3 Create use-case implementations in `application/usecase/tag/`: `AddTagToAssetUseCaseImpl`, `RemoveTagFromAssetUseCaseImpl`, `BulkAddTagUseCaseImpl`, `BulkRemoveTagUseCaseImpl`, `ListTagsUseCaseImpl`; each annotated `@Service @Transactional`; inject only `AssetRepository` and `TagRepository`; normalize tag name to lowercase, create-or-find tag, manage join table entries, delete orphan tags after removal
- [x] 2.4 Add `Set<String> tags` field to `application/dto/AssetFilter.java`; update the `AssetRepositoryImpl` Criteria API implementation to apply the `tags` AND predicate when `filter.getTags()` is non-empty (AND semantics via `GROUP BY / HAVING COUNT`)

## 3. Backend: API Endpoints

- [x] 3.1 Add `POST /api/assets/{id}/tags` endpoint to `infrastructure/web/controller/AssetController`; inject `AddTagToAssetUseCase`; delegate to `addTagUseCase.execute(id, dto.name())`; return `201 Created`
- [x] 3.2 Add `DELETE /api/assets/{id}/tags?name=...` endpoint to `infrastructure/web/controller/AssetController`; inject `RemoveTagFromAssetUseCase`; return `204 No Content` or `404`
- [x] 3.3 Create `infrastructure/web/controller/TagController.java` with `GET /api/tags?q=...`; inject `ListTagsUseCase`; return `List<String>` capped at 20
- [x] 3.4 Add `POST /api/assets/tags/bulk` and `DELETE /api/assets/tags/bulk` endpoints to `infrastructure/web/controller/AssetController`; inject `BulkAddTagUseCase` and `BulkRemoveTagUseCase`
- [x] 3.5 Add `tags` query parameter to `GET /api/assets` endpoint (comma-separated); forward to `AssetFilter.tags` in the request mapping in `AssetController`
- [x] 3.6 Add `List<String> tags` field to `infrastructure/web/dto/AssetDto`; update `AssetDtoMapper` in `infrastructure/web/mapper/` to map `asset.getTags()` from the domain model

## 4. Backend: Tests

- [x] 4.1 Unit-test each use-case implementation (`AddTagToAssetUseCaseImpl`, `RemoveTagFromAssetUseCaseImpl`, `BulkAddTagUseCaseImpl`, `BulkRemoveTagUseCaseImpl`): add (new tag, existing tag, duplicate idempotency), remove (orphan cleanup, tag still used), bulk operations; mock `AssetRepository` and `TagRepository`
- [x] 4.2 Unit-test the updated `AssetRepositoryImpl` filter for the `tags` AND predicate
- [x] 4.3 Write `@WebMvcTest` for `AssetController` (tag endpoints: `POST /api/assets/{id}/tags`, `DELETE /api/assets/{id}/tags`, bulk endpoints, `GET /api/assets?tags=`) and `TagController` (`GET /api/tags`); mock each use-case interface
- [x] 4.4 Write an integration test (`@SpringBootTest` + Testcontainers) covering the full add-tag → filter-by-tag → remove-tag lifecycle

## 5. Frontend: Models and Service

- [x] 5.1 Add `Tag` interface (`{ name: string }`) to `core/models/`
- [x] 5.2 Add `tags: string[]` field to the `Asset` interface in `core/models/asset.model.ts`
- [x] 5.3 Create `TagService` in `core/services/` with methods `addTag(assetId, name)`, `removeTag(assetId, name)`, `searchTags(q)`, `bulkAddTag(assetIds, name)`, `bulkRemoveTag(assetIds, name)`
- [x] 5.4 Extend `AssetService.getAssets()` to accept and forward an optional `tags: string[]` parameter

## 6. Frontend: Tag Chip Editor (EXIF Panel)

- [x] 6.1 Add `MatChipsModule` and `MatAutocompleteModule` imports to `ExifPanelComponent`
- [x] 6.2 Add a tag chip list with `MatChipInput` to the EXIF panel template; bind chip display to `asset.tags`
- [x] 6.3 Wire the chip input to call `TagService.addTag()` on Enter/comma and `TagService.removeTag()` on chip × click, updating `asset.tags` optimistically
- [x] 6.4 Wire `MatAutocomplete` to debounce-call `TagService.searchTags(q)` as the user types
- [x] 6.5 Write a Cypress component test for the tag chip editor: displays existing tags, adds a tag (stub service), removes a tag, autocomplete suggestions appear

## 7. Frontend: Tag Filter in Gallery Toolbar

- [x] 7.1 Add a tag multi-select chip input to the gallery filter toolbar (below search / date pickers)
- [x] 7.2 Add `selectedTags: string[]` state to `GalleryComponent`; pass to `AssetService.getAssets()` as the `tags` param
- [x] 7.3 Ensure selecting or removing a tag chip resets `pageIndex` to 0 and reloads assets
- [x] 7.4 Ensure the existing clear-filters action also clears `selectedTags`
- [x] 7.5 Write a Cypress component test for the tag filter: selecting a tag triggers filtered load, removing a chip reverts, clear-all removes tag chips

## 8. Frontend: Bulk Tag Dialog

- [x] 8.1 Create `features/gallery/bulk-tag-dialog/bulk-tag-dialog.component.ts` as a standalone `MatDialog` component
- [x] 8.2 Dialog accepts `assetIds: number[]` as dialog data; shows a chip input for tags to add and a list of common tags to remove
- [x] 8.3 On confirm, call `TagService.bulkAddTag()` and/or `TagService.bulkRemoveTag()` then close the dialog
- [x] 8.4 Add "Tag selected" button to the gallery selection-mode toolbar; open `BulkTagDialogComponent` passing selected asset IDs
- [x] 8.5 Write a Cypress component test for `BulkTagDialogComponent`: renders inputs, calls bulk service methods on confirm, closes on cancel

## 9. Integration Verification

- [x] 9.1 Run `mvn test` in the backend — all tests pass
- [x] 9.2 Run `npm test` in the frontend — all Cypress component tests pass
- [x] 9.3 Run `npm run lint` — no lint errors (ESLint not configured; tsc --noEmit passes with only 1 pre-existing error)
- [x] 9.4 Start the app locally and manually verify: add a tag in the EXIF panel, filter gallery by tag (AND semantics), clear filter, bulk-tag 3 assets, verify tag autocomplete
