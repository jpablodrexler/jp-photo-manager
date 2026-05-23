## 1. Dependency

- [ ] 1.1 Add `org.apache.commons:commons-compress` to `pom.xml`

## 2. ArchiveService

- [ ] 2.1 Create `infrastructure/service/ArchiveService.java` annotated with `@Service`
- [ ] 2.2 Implement `void extractImages(Path archivePath, BiConsumer<Path, String> consumer)`:
  - If `.zip` extension: use `ZipInputStream` to iterate entries; extract image entries to temp dir; call `consumer(tempFile, virtualFolderPath)`
  - If `.tar.gz` / `.tgz` extension: use `TarArchiveInputStream` wrapped in `GzipCompressorInputStream`; same logic
  - Virtual folder path format: `archivePath.toString() + "!/" + entryDirectoryPath`
  - Delete temp directory in `finally`
- [ ] 2.3 Implement `void writeTarGz(List<Path> files, OutputStream out)`: wrap `out` in `GzipCompressorOutputStream` + `TarArchiveOutputStream`; add each file as a `TarArchiveEntry`

## 3. CatalogAssetsUseCaseImpl — archive routing

- [ ] 3.1 Detect `.zip`, `.tar.gz`, `.tgz` extensions during folder scan
- [ ] 3.2 For archive files: call `archiveService.extractImages(archivePath, consumer)` where the consumer processes each extracted image through the standard catalog path with the virtual `folderPath`

## 4. Download endpoint extension

- [ ] 4.1 Add `@RequestParam(required = false, defaultValue = "zip") String format` to `GET /api/assets/download`
- [ ] 4.2 If `format = "tar.gz"`: use `archiveService.writeTarGz(resolvedFilePaths, response.getOutputStream())`; set `Content-Type: application/gzip` and `Content-Disposition: attachment; filename="download.tar.gz"`

## 5. Backend unit tests

- [ ] 5.1 Test that `ArchiveService.extractImages()` produces the correct virtual folder paths from a zip
- [ ] 5.2 Test that `ArchiveService.extractImages()` works for a tar.gz archive
- [ ] 5.3 Test that `GET /api/assets/download?format=tar.gz` returns `Content-Type: application/gzip`
- [ ] 5.4 Test that `GET /api/assets/download` (no format) still returns zip (backward compatible)

## 6. Frontend — FolderNavComponent

- [ ] 6.1 In `FolderNavComponent`, detect `node.path.includes('!')` and render a `folder_zip` Material icon instead of the standard `folder` icon

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
