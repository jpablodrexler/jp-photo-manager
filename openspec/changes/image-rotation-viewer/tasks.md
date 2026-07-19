## 1. ThumbnailComponent — rotation style

- [ ] 1.1 Add `getRotationStyle(imageRotation: string | null): string` method to `ThumbnailComponent` mapping `"ROTATE_0"` → `"rotate(0deg)"`, `"ROTATE_90"` → `"rotate(90deg)"`, `"ROTATE_180"` → `"rotate(180deg)"`, `"ROTATE_270"` → `"rotate(270deg)"`, and `null`/`undefined` → `"rotate(0deg)"`
- [ ] 1.2 Add `[style.transform]="getRotationStyle(asset.imageRotation)"` to the `<img>` element in `thumbnail.component.html`
- [ ] 1.3 Add `object-fit: contain; max-width: 100%; max-height: 100%` to the thumbnail `<img>` CSS to handle dimension changes caused by rotation

## 2. GalleryComponent viewer — rotation style

- [ ] 2.1 Add or reuse the same `getRotationStyle()` method in `GalleryComponent` (or extract to a shared utility function)
- [ ] 2.2 Add `[style.transform]="getRotationStyle(selectedAsset?.imageRotation)"` to the full-size viewer `<img>` in `gallery.component.html`

## 3. Frontend tests

- [ ] 3.1 Cypress component test: `ThumbnailComponent` with `imageRotation: "ROTATE_90"` renders `<img>` with `transform: rotate(90deg)`
- [ ] 3.2 Cypress component test: `ThumbnailComponent` with `imageRotation: "ROTATE_0"` renders `<img>` with `transform: rotate(0deg)`
- [ ] 3.3 Cypress component test: `ThumbnailComponent` with `imageRotation: null` renders `<img>` with no unexpected transform

## 4. Testing and Commit

- [ ] 4.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 4.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 4.3 Commit all changes (only after both test suites pass)
