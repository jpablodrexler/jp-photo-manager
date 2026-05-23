## 1. Flyway migration V17

- [ ] 1.1 Create `V17__full_text_search.sql` (must run after V16 which adds `description`):
  ```sql
  ALTER TABLE assets ADD COLUMN search_vector TSVECTOR;
  CREATE INDEX idx_assets_search_vector ON assets USING GIN(search_vector);
  ```
- [ ] 1.2 Create a PostgreSQL function `assets_search_vector_trigger()` that computes:
  ```sql
  NEW.search_vector :=
    setweight(to_tsvector('english', coalesce(NEW.file_name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B');
  ```
  Note: tag and EXIF data join requires a separate trigger on `asset_tags` and `asset_exif`
- [ ] 1.3 Create trigger: `CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON assets FOR EACH ROW EXECUTE FUNCTION assets_search_vector_trigger()`
- [ ] 1.4 Backfill existing rows: `UPDATE assets SET search_vector = setweight(to_tsvector('english', coalesce(file_name, '')), 'A') || setweight(to_tsvector('english', coalesce(description, '')), 'B') WHERE deleted_at IS NULL`
- [ ] 1.5 Create a separate trigger on `asset_tags` to update `search_vector` on the parent `asset` when tags change

## 2. Repository — update findByFolderWithFilters

- [ ] 2.1 In `JpaAssetRepository`, update the `@Query` for `findByFolderWithFilters` to add:
  ```sql
  AND (:search IS NULL OR LENGTH(:search) < 3
    OR a.search_vector @@ websearch_to_tsquery('english', :search)
    OR LOWER(a.file_name) LIKE LOWER(CONCAT('%', :search, '%')))
  ```
- [ ] 2.2 When `search` is provided and >= 3 characters, order by `ts_rank(a.search_vector, websearch_to_tsquery('english', :search)) DESC` before the existing secondary sort

## 3. Backend unit tests

- [ ] 3.1 Test that a search for a camera model term returns assets with that model in EXIF
- [ ] 3.2 Test that a 2-character search uses LIKE fallback (no `websearch_to_tsquery` syntax error)
- [ ] 3.3 Test that a multi-word search returns assets matching any lexeme

## 4. Testing and Commit

- [ ] 4.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 4.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 4.3 Commit all changes (only after both test suites pass)
