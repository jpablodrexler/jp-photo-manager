## 1. Backend: Database Schema

- [ ] 1.1 Write Flyway migration `V<next>__create_tags_tables.sql` creating the `tags` table (`tag_id SERIAL PK`, `name VARCHAR(100) NOT NULL UNIQUE`) and the `asset_tags` join table (`asset_id BIGINT FK`, `tag_id BIGINT FK`, `PK(asset_id, tag_id)`)
- [ ] 1.2 Create `Tag` JPA entity in `domain/entity/` with `@ManyToMany` relationship to `Asset`
- [ ] 1.3 Add `Set<Tag> tags` field to the `Asset` entity with the appropriate `@ManyToMany(fetch = LAZY)` and `@JoinTable` annotation

## 2. Backend: Repository and Domain Service

- [ ] 2.1 Create `TagRepository` extending `JpaRepository<Tag, Long>` with a `findByNameContainingIgnoreCase(String q, Pageable p)` query method
- [ ] 2.2 Add `addTag`, `removeTag`, `bulkAddTag`, `bulkRemoveTag`, and `listTags` method signatures to the `PhotoManagerFacade` interface
- [ ] 2.3 Implement these methods in `PhotoManagerFacadeImpl`: normalize tag name to lowercase, create-or-find tag, manage join table entries, delete orphan tags after removal
- [ ] 2.4 Extend the existing Criteria API asset filter in `AssetRepository` to accept an optional `Set<String> tags` predicate (AND semantics via `GROUP BY / HAVING COUNT`)

## 3. Backend: API Endpoints

- [ ] 3.1 Add `POST /api/assets/{id}/tags` endpoint to `AssetController` accepting `{ "name": "..." }` body; delegates to facade; returns `201 Created`
- [ ] 3.2 Add `DELETE /api/assets/{id}/tags?name=...` endpoint to `AssetController`; returns `204 No Content` or `404`
- [ ] 3.3 Add `GET /api/tags?q=...` endpoint (new `TagController` or additional mapping in `AssetController`); returns `List<String>` capped at 20
- [ ] 3.4 Add `POST /api/assets/tags/bulk` and `DELETE /api/assets/tags/bulk` endpoints for bulk tag add/remove across a list of asset IDs
- [ ] 3.5 Add `tags` query parameter to `GET /api/assets` endpoint (comma-separated); forward to the updated facade method
- [ ] 3.6 Add `List<String> tags` field to `AssetDto` and populate it in the `toDto()` mapper

## 4. Backend: Tests

- [ ] 4.1 Unit-test `PhotoManagerFacadeImpl` tag methods: add (new tag, existing tag, duplicate idempotency), remove (orphan cleanup, tag still used), bulk operations
- [ ] 4.2 Unit-test the updated Criteria API filter for the `tags` AND predicate
- [ ] 4.3 Write `@WebMvcTest` for the tag endpoints: `POST`, `DELETE`, `GET /api/tags`, bulk endpoints, and `GET /api/assets?tags=`
- [ ] 4.4 Write an integration test (`@SpringBootTest` + Testcontainers) covering the full add-tag → filter-by-tag → remove-tag lifecycle

## 5. Frontend: Models and Service

- [ ] 5.1 Add `Tag` interface (`{ name: string }`) to `core/models/`
- [ ] 5.2 Add `tags: string[]` field to the `Asset` interface in `core/models/asset.model.ts`
- [ ] 5.3 Create `TagService` in `core/services/` with methods `addTag(assetId, name)`, `removeTag(assetId, name)`, `searchTags(q)`, `bulkAddTag(assetIds, name)`, `bulkRemoveTag(assetIds, name)`
- [ ] 5.4 Extend `AssetService.getAssets()` to accept and forward an optional `tags: string[]` parameter

## 6. Frontend: Tag Chip Editor (EXIF Panel)

- [ ] 6.1 Add `MatChipsModule` and `MatAutocompleteModule` imports to `ExifPanelComponent`
- [ ] 6.2 Add a tag chip list with `MatChipInput` to the EXIF panel template; bind chip display to `asset.tags`
- [ ] 6.3 Wire the chip input to call `TagService.addTag()` on Enter/comma and `TagService.removeTag()` on chip × click, updating `asset.tags` optimistically
- [ ] 6.4 Wire `MatAutocomplete` to debounce-call `TagService.searchTags(q)` as the user types
- [ ] 6.5 Write a Cypress component test for the tag chip editor: displays existing tags, adds a tag (stub service), removes a tag, autocomplete suggestions appear

## 7. Frontend: Tag Filter in Gallery Toolbar

- [ ] 7.1 Add a tag multi-select chip input to the gallery filter toolbar (below search / date pickers)
- [ ] 7.2 Add `selectedTags: string[]` state to `GalleryComponent`; pass to `AssetService.getAssets()` as the `tags` param
- [ ] 7.3 Ensure selecting or removing a tag chip resets `pageIndex` to 0 and reloads assets
- [ ] 7.4 Ensure the existing clear-filters action also clears `selectedTags`
- [ ] 7.5 Write a Cypress component test for the tag filter: selecting a tag triggers filtered load, removing a chip reverts, clear-all removes tag chips

## 8. Frontend: Bulk Tag Dialog

- [ ] 8.1 Create `features/gallery/bulk-tag-dialog/bulk-tag-dialog.component.ts` as a standalone `MatDialog` component
- [ ] 8.2 Dialog accepts `assetIds: number[]` as dialog data; shows a chip input for tags to add and a list of common tags to remove
- [ ] 8.3 On confirm, call `TagService.bulkAddTag()` and/or `TagService.bulkRemoveTag()` then close the dialog
- [ ] 8.4 Add "Tag selected" button to the gallery selection-mode toolbar; open `BulkTagDialogComponent` passing selected asset IDs
- [ ] 8.5 Write a Cypress component test for `BulkTagDialogComponent`: renders inputs, calls bulk service methods on confirm, closes on cancel

## 9. Integration Verification

- [ ] 9.1 Run `mvn test` in the backend — all tests pass
- [ ] 9.2 Run `npm test` in the frontend — all Cypress component tests pass
- [ ] 9.3 Run `npm run lint` — no lint errors
- [ ] 9.4 Start the app locally and manually verify: add a tag in the EXIF panel, filter gallery by tag (AND semantics), clear filter, bulk-tag 3 assets, verify tag autocomplete
