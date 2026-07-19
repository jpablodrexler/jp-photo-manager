## 1. `KeyboardService` — core implementation

- [ ] 1.1 Create `JPPhotoManagerWeb/frontend/src/app/core/services/keyboard.service.ts` as an `@Injectable({ providedIn: 'root' })` class; add private fields `private destroy$ = new Subject<void>()`, `readonly rateSelected$ = new Subject<number>()`, `readonly deleteSelected$ = new Subject<void>()`, `readonly focusSearch$ = new Subject<void>()`; inject `MatDialog` in the constructor
- [ ] 1.2 Implement `private isInputFocused(event: KeyboardEvent): boolean` that returns `true` when `event.target` is an `HTMLInputElement`, `HTMLTextAreaElement`, or has `isContentEditable === true`
- [ ] 1.3 Implement `init(router: Router): void` that subscribes to `fromEvent<KeyboardEvent>(document, 'keydown').pipe(takeUntil(this.destroy$))` and calls the private `handleKey(event, router)` handler
- [ ] 1.4 Implement `private handleKey(event: KeyboardEvent, router: Router): void` with a `switch (event.key)` block:
  - Return immediately (without `switch`) if `this.isInputFocused(event)` is true
  - Case `'g'` / `'G'`: `router.navigateByUrl('/gallery')`
  - Case `'a'` / `'A'`: `router.navigateByUrl('/albums')`
  - Case `'d'` / `'D'`: `router.navigateByUrl('/duplicates')`
  - Cases `'1'` through `'5'`: `this.rateSelected$.next(Number(event.key))`
  - Case `'Delete'`: `this.deleteSelected$.next()`
  - Case `'/'`: `event.preventDefault(); this.focusSearch$.next()`
  - Case `'?'`: `this.dialog.open(KeyboardShortcutsHelpComponent, { width: '640px', maxHeight: '90vh' })`
- [ ] 1.5 Implement `destroy(): void` that calls `this.destroy$.next()` and `this.destroy$.complete()`

## 2. `KeyboardShortcutsHelpComponent` — help overlay

- [ ] 2.1 Create directory `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/`
- [ ] 2.2 Create `keyboard-shortcuts-help.component.ts` as a standalone component with selector `app-keyboard-shortcuts-help`; import `MatDialogModule`, `MatButtonModule`, `MatIconModule`; inject `MatDialogRef<KeyboardShortcutsHelpComponent>` to support programmatic close
- [ ] 2.3 Create `keyboard-shortcuts-help.component.html` with:
  - A `<h2 mat-dialog-title>` containing "Keyboard Shortcuts"
  - A `<mat-dialog-content>` containing two sections, each with a `<h3>` ("Global" and "Gallery") and a `<table>` with columns "Key" and "Description"
  - **Global** rows: `G` → Navigate to Gallery; `A` → Navigate to Albums; `D` → Navigate to Duplicates; `/` → Focus search input; `?` → Show this help overlay
  - **Gallery** rows: `1`–`5` → Rate current / selected asset; `0` or same digit again → Clear rating; `Del` → Remove from catalog (soft-delete); `Space` → Pause / resume slideshow; `←` / `→` → Previous / next asset; `Esc` → Exit slideshow
  - A `<mat-dialog-actions align="end">` with a "Close" `mat-button` that calls `dialogRef.close()`
- [ ] 2.4 Create `keyboard-shortcuts-help.component.scss` with styles for the shortcut table: `kbd` elements styled with a monospace font, light border, and slight padding; table rows with alternating background via `:nth-child(even)`

## 3. `AppComponent` integration

- [ ] 3.1 In `app.component.ts`, inject `KeyboardService` and `Router` in the constructor (both are already available as project dependencies)
- [ ] 3.2 Call `this.keyboardService.init(this.router)` at the start of `ngOnInit()`, before the existing `breakpointObserver` subscription
- [ ] 3.3 Call `this.keyboardService.destroy()` inside `ngOnDestroy()`, after the existing `this.bpSub.unsubscribe()`
- [ ] 3.4 Verify that `AppComponent` imports are unchanged (no new Angular Material imports needed — `MatDialog` is owned by the service)

## 4. `GalleryComponent` integration — rating shortcut

- [ ] 4.1 In `gallery.component.ts`, inject `KeyboardService` in the constructor
- [ ] 4.2 In `ngOnInit()`, subscribe to `this.keyboardService.rateSelected$.pipe(takeUntil(this.destroy$))` with a handler that:
  - If `viewMode` is `'viewer'` or `'slideshow'` and `currentViewerAsset` exists: calls `this.rateCurrentAsset(star)`
  - If `viewMode` is `'thumbnails'` and exactly one asset is selected: calls `this.rateAsset(theSelectedAsset, star)`
  - Otherwise: does nothing
- [ ] 4.3 Confirm `this.destroy$` is already defined in `GalleryComponent` (it is — used for `tagFilterControl`) so no new subject is needed

## 5. `GalleryComponent` integration — delete shortcut

- [ ] 5.1 In `ngOnInit()`, subscribe to `this.keyboardService.deleteSelected$.pipe(takeUntil(this.destroy$))` with a handler that calls `this.deleteSelected(false)` (soft-delete, `deleteFiles = false`) when `this.selectedAssets.size > 0`, and is a no-op otherwise

## 6. `GalleryComponent` integration — search-focus shortcut

- [ ] 6.1 In `gallery.component.html`, add the template reference variable `#searchInput` to the search `<mat-form-field>` inner `<input>` element (the `matInput` for the search term)
- [ ] 6.2 In `gallery.component.ts`, add `@ViewChild('searchInput') private searchInput!: ElementRef<HTMLInputElement>`
- [ ] 6.3 In `ngOnInit()`, subscribe to `this.keyboardService.focusSearch$.pipe(takeUntil(this.destroy$))` with a handler that calls `this.searchInput?.nativeElement?.focus()`

## 7. Frontend — Unit / Cypress tests for `KeyboardService`

- [ ] 7.1 Create `JPPhotoManagerWeb/frontend/src/app/core/services/keyboard.service.cy.ts` using `cy.mount` with a minimal blank fixture component
- [ ] 7.2 Test `isInputFocused_inputElement_returnsTrue`: create an `<input>` element, dispatch a `keydown` event from it, verify the shortcut guard suppresses the action
- [ ] 7.3 Test `init_gKeyPressed_navigatesToGallery`: stub `Router.navigateByUrl`; call `service.init(routerStub)`; dispatch `keydown` with `key = 'g'`; assert `routerStub.navigateByUrl` was called with `'/gallery'`
- [ ] 7.4 Test `init_aKeyPressed_navigatesToAlbums`: same pattern for `'a'` → `'/albums'`
- [ ] 7.5 Test `init_dKeyPressed_navigatesToDuplicates`: same pattern for `'d'` → `'/duplicates'`
- [ ] 7.6 Test `init_digitKey_emitsRateSelected`: subscribe to `rateSelected$`; dispatch `keydown` with `key = '3'`; assert emitted value is `3`
- [ ] 7.7 Test `init_deleteKey_emitsDeleteSelected`: subscribe to `deleteSelected$`; dispatch `keydown` with `key = 'Delete'`; assert subject emitted
- [ ] 7.8 Test `init_slashKey_emitsFocusSearch`: subscribe to `focusSearch$`; dispatch `keydown` with `key = '/'`; assert subject emitted
- [ ] 7.9 Test `init_questionMark_opensHelpDialog`: stub `MatDialog.open`; dispatch `keydown` with `key = '?'`; assert `dialog.open` was called with `KeyboardShortcutsHelpComponent`
- [ ] 7.10 Test `init_keyInInput_suppressesNavigation`: focus an `<input>` element; dispatch `keydown` with `key = 'g'` targeting that element; assert `router.navigateByUrl` was NOT called
- [ ] 7.11 Test `destroy_afterDestroy_stopsHandlingKeys`: call `service.destroy()`; dispatch `keydown` with `key = 'g'`; assert `router.navigateByUrl` was NOT called

## 8. Frontend — Cypress tests for `KeyboardShortcutsHelpComponent`

- [ ] 8.1 Create `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/keyboard-shortcuts-help.component.cy.ts`
- [ ] 8.2 Test `mount_displaysGlobalSection_withAllGlobalShortcuts`: mount the component; assert `h3` with text "Global" exists; assert rows for `G`, `A`, `D`, `/`, `?` are present
- [ ] 8.3 Test `mount_displaysGallerySection_withAllGalleryShortcuts`: assert `h3` with text "Gallery" exists; assert rows for `1`–`5`, `Del`, `Space`, arrows, `Esc` are present
- [ ] 8.4 Test `closeButton_click_closesDialog`: stub `MatDialogRef.close`; click the "Close" button; assert `dialogRef.close` was called

## 9. Testing and Commit

- [ ] 9.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 9.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 9.3 Commit all changes (only after both test suites pass)
