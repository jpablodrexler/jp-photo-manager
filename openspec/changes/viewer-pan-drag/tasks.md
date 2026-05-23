## 1. Viewer component — pan state

- [ ] 1.1 Add properties to the viewer component: `panX = 0`, `panY = 0`, `isDragging = false`, `lastTouchX = 0`, `lastTouchY = 0`
- [ ] 1.2 Add `@ViewChild` reference to the viewer container element

## 2. Mouse drag event handlers

- [ ] 2.1 Bind `(mousedown)` on the viewer container: set `isDragging = true`; call `$event.preventDefault()` to prevent browser ghost drag
- [ ] 2.2 Bind `(mousemove)` on the viewer container: if `isDragging`, `panX += $event.movementX / viewerZoom`, `panY += $event.movementY / viewerZoom`
- [ ] 2.3 Bind `(mouseup)` on the viewer container: set `isDragging = false`
- [ ] 2.4 Bind `(mouseleave)` on the viewer container: set `isDragging = false`

## 3. Touch event handlers

- [ ] 3.1 Bind `(touchstart)` on the viewer container: record `lastTouchX = $event.touches[0].clientX`, `lastTouchY = $event.touches[0].clientY`; set `isDragging = true`
- [ ] 3.2 Bind `(touchmove)` on the viewer container: compute delta from `lastTouchX/Y`; update `panX` and `panY`; update `lastTouchX/Y`; call `$event.preventDefault()`
- [ ] 3.3 Bind `(touchend)` on the viewer container: set `isDragging = false`

## 4. Transform binding

- [ ] 4.1 Update the `<img>` transform binding from `scale(viewerZoom)` to `scale({{ viewerZoom }}) translate({{ panX }}px, {{ panY }}px)`
- [ ] 4.2 Add `draggable="false"` attribute to the `<img>` to prevent browser native drag behavior

## 5. Cursor styling

- [ ] 5.1 Add to viewer component SCSS: `.viewer-container { cursor: grab; user-select: none; }` and `.viewer-container.dragging { cursor: grabbing; }`
- [ ] 5.2 Bind `[class.dragging]="isDragging"` on the viewer container

## 6. Auto-reset

- [ ] 6.1 In the zoom change handler: `if (viewerZoom === 1) { panX = 0; panY = 0; }`
- [ ] 6.2 In the asset change handler (`ngOnChanges` or setter on the asset input): `panX = 0; panY = 0;`

## 7. Frontend tests

- [ ] 7.1 Add a Cypress component test verifying that simulating a `mousemove` event while `isDragging` updates `panX` and `panY`
- [ ] 7.2 Add a test verifying that `panX` and `panY` reset to 0 when `viewerZoom` is set to 1

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
