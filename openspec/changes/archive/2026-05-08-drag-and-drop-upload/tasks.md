## 1. Backend — Exception and DTO

- [x] 1.1 Create `FolderNotFoundException.java` in `api/exception/` as a plain `RuntimeException` subclass with a constructor `FolderNotFoundException(String folderPath)` that sets a message like `"Folder not found in catalog: " + folderPath`
- [x] 1.2 Create `UploadRequest.java` in `api/dto/` as a Java record with a single `@NotBlank String folderPath` field; this record is bound alongside the multipart file in the controller

## 2. Backend — Application layer

- [x] 2.1 Add `Asset uploadAsset(String folderPath, MultipartFile file) throws IOException;` to the `PhotoManagerFacade` interface
- [x] 2.2 Implement `PhotoManagerFacadeImpl.uploadAsset`: call `folderRepository.existsByPath(folderPath)`; throw `FolderNotFoundException(folderPath)` if false
- [x] 2.3 Generate a unique temp file path: `Path tempFile = Files.createTempFile(UUID.randomUUID().toString() + "_", "_" + file.getOriginalFilename())`
- [x] 2.4 Call `file.transferTo(tempFile.toFile())` to write the uploaded bytes; wrap in try-finally that calls `Files.deleteIfExists(tempFile)` on both success and failure
- [x] 2.5 Build the destination path: `String destPath = folderPath + "/" + file.getOriginalFilename()`; call `storageService.copyFile(tempFile.toString(), destPath)`
- [x] 2.6 Call `catalogFolderService.createAsset(folderPath, file.getOriginalFilename())` and return the resulting `Asset`

## 3. Backend — API layer

- [x] 3.1 Add a `POST /api/assets/upload` handler to `AssetController` with parameters `@RequestPart("file") MultipartFile file` and `@RequestPart("folderPath") String folderPath`
- [x] 3.2 In the handler, validate the file's content-type against the allowlist `{image/jpeg, image/png, image/gif, image/bmp, image/tiff, image/webp}`; also validate that the original filename extension (lowercased) is in `{jpg, jpeg, png, gif, bmp, tiff, tif, webp}`; return `ResponseEntity.status(415).build()` if either check fails
- [x] 3.3 Delegate to `facade.uploadAsset(folderPath, file)`, map the result with the existing `toDto(Asset)` helper, and return `ResponseEntity.status(201).body(dto)`
- [x] 3.4 Catch `FolderNotFoundException` and return `ResponseEntity.notFound().build()`
- [x] 3.5 Catch `IOException` and return `ResponseEntity.status(500).build()`

## 4. Backend — Tests

- [x] 4.1 Create `AssetControllerUploadTest` (`@WebMvcTest`): mock `PhotoManagerFacade`; assert `POST /api/assets/upload` with a valid JPEG multipart returns `201` with the expected `AssetDto` JSON
- [x] 4.2 Add test: upload a non-image file (e.g., content-type `text/plain`) returns `415`
- [x] 4.3 Add test: `folderPath` that causes `FolderNotFoundException` returns `404`
- [x] 4.4 Create `UploadAssetIntegrationTest` (`@SpringBootTest`): catalog a temp folder, upload a real JPEG test fixture to it via `MockMvc`, assert `201` response and that `GET /api/assets?folderPath=` returns the new asset
- [x] 4.5 Run `mvn test` and confirm all tests pass

## 5. Frontend — AssetService

- [x] 5.1 Add `uploadAsset(folderPath: string, file: File): Observable<HttpEvent<AssetDto>>` to `asset.service.ts`; build a `FormData` with `file` and `folderPath` fields; call `this.http.post<AssetDto>('/api/assets/upload', formData, { reportProgress: true, observe: 'events' })` and return the observable

## 6. Frontend — DropZoneComponent

- [x] 6.1 Create `frontend/src/app/features/gallery/drop-zone/` directory with `drop-zone.component.ts`, `.html`, `.scss`, `.cy.ts`
- [x] 6.2 Declare the standalone component with `@Input() folderPath!: string` and `@Output() uploadComplete = new EventEmitter<void>()`
- [x] 6.3 Add component state: `isDragging = false`, `uploadQueue: { file: File; progress: number; status: 'pending' | 'uploading' | 'done' | 'error' }[] = []`
- [x] 6.4 Add `@HostListener('dragover', ['$event'])` that calls `event.preventDefault()` and sets `isDragging = true`; add `@HostListener('dragleave')` that sets `isDragging = false`; add `@HostListener('drop', ['$event'])` that calls `event.preventDefault()`, sets `isDragging = false`, and calls `onFilesSelected(event.dataTransfer!.files)`
- [x] 6.5 Add `onFilesSelected(fileList: FileList)` method: filter accepted image extensions, push each to `uploadQueue`, then call `processQueue()`
- [x] 6.6 Add `processQueue()` method: iterate `uploadQueue` items with `status === 'pending'` sequentially (using `lastValueFrom` or a recursive `subscribe`); for each call `assetService.uploadAsset(folderPath, file)`, update `progress` from `HttpEventType.UploadProgress` events, set `status: 'done'` on `HttpEventType.Response`; set `status: 'error'` on error; after all items are processed emit `uploadComplete`
- [x] 6.7 Add `triggerFileInput()` method that calls `this.fileInput.nativeElement.click()` on a `@ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>`
- [x] 6.8 Build the template: a drag-active overlay `<div class="drop-overlay">Drop images here</div>` shown with `@if (isDragging)`; an "Upload" `mat-icon-button` that calls `triggerFileInput()`; a hidden `<input #fileInput type="file" accept="image/*" multiple (change)="onFilesSelected($event.target.files)">`; a `@for (item of uploadQueue)` list showing filename, `MatProgressBar` with `[value]="item.progress"`, and a status icon
- [x] 6.9 Write SCSS: `.drop-overlay` as `position: absolute; inset: 0; background: rgba(0,0,0,0.6); border: 2px dashed #4caf50; display: flex; align-items: center; justify-content: center; z-index: 20; font-size: 24px; color: white`; `.upload-queue` list layout below the grid
- [x] 6.10 Import `MatIconModule`, `MatButtonModule`, `MatProgressBarModule` in the component's `imports` array

## 7. Frontend — GalleryComponent wiring

- [x] 7.1 Import `DropZoneComponent` in `GalleryComponent`'s `imports` array
- [x] 7.2 Add `<app-drop-zone [folderPath]="currentFolder" (uploadComplete)="onUploadComplete()" />` inside the `.thumbnail-grid-container` div (positioned absolutely so it overlays the grid)
- [x] 7.3 Add `onUploadComplete()` method to `GalleryComponent`: reset `pageIndex = 0` and call `loadAssets()` to refresh the grid
- [x] 7.4 In `gallery.component.scss`, add `position: relative` to `.thumbnail-grid-container` so the absolute-positioned overlay is scoped correctly

## 8. Frontend — Tests

- [x] 8.1 In `drop-zone.component.cy.ts`, add test: component mounts with `folderPath` input; no upload queue visible initially
- [x] 8.2 Add test: triggering `dragover` event sets `isDragging = true` and shows `.drop-overlay`
- [x] 8.3 Add test: triggering `dragleave` hides `.drop-overlay`
- [x] 8.4 Add test: dropping a non-image file (e.g., `.txt`) does not add it to the queue
- [x] 8.5 Add test: dropping a valid JPEG file adds a queue item; `assetService.uploadAsset` stub is called with the correct `folderPath`; `MatProgressBar` appears
- [x] 8.6 Add test: when the upload stub resolves successfully, the item's status icon shows "done" and `uploadComplete` is emitted
- [x] 8.7 Run `npm test` and confirm all tests pass
