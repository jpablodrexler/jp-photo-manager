## 1. Database migration

- [ ] 1.1 Create `V23__add_target_format_to_convert_definitions.sql`: `ALTER TABLE convert_assets_directories_definitions ADD COLUMN target_format VARCHAR(10) NOT NULL DEFAULT 'JPEG'`

## 2. Domain — ImageFormat enum

- [ ] 2.1 Create `domain/enums/ImageFormat.java` with values `JPEG`, `WEBP`, `AVIF`
- [ ] 2.2 Add `targetFormat` field to the `ConvertDirectoryDefinition` domain model; default to `JPEG`

## 3. ConvertAssetsServiceImpl update

- [ ] 3.1 Inject the `targetFormat` from the directory definition into the per-file conversion logic
- [ ] 3.2 Add `convertToWebP(Path input, Path output)`: `ProcessBuilder(["cwebp", "-q", "80", input.toString(), "-o", output.toString()])` with `waitFor(5, TimeUnit.MINUTES)`
- [ ] 3.3 Add `convertToAvif(Path input, Path output)`: `ProcessBuilder(["avifenc", "--speed", "6", input.toString(), output.toString()])` with `waitFor(10, TimeUnit.MINUTES)`
- [ ] 3.4 In the per-file loop, dispatch on `targetFormat`: `JPEG` → existing path, `WEBP` → `convertToWebP()`, `AVIF` → `convertToAvif()`
- [ ] 3.5 Catch process exit code ≠ 0 per asset; log error and mark as failed; continue loop

## 4. Docker image

- [ ] 4.1 Add to the backend `Dockerfile`: `RUN apt-get update && apt-get install -y webp libavif-apps && rm -rf /var/lib/apt/lists/*`

## 5. Backend unit tests

- [ ] 5.1 Test that `ConvertAssetsServiceImpl` invokes the `cwebp` command for `WEBP` format
- [ ] 5.2 Test that `ConvertAssetsServiceImpl` invokes the `avifenc` command for `AVIF` format
- [ ] 5.3 Test that a non-zero encoder exit code logs an error but does not throw

## 6. Frontend — ConvertComponent

- [ ] 6.1 Add `targetFormat` field to the directory pair model in `ConvertComponent`
- [ ] 6.2 Add a `MatSelect` with options `JPEG`, `WebP`, `AVIF` to the directory pair form
- [ ] 6.3 Include `targetFormat` in the API request body when saving a pair

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
