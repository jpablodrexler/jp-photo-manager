# Cypress Unit Test Developer — TypeScript Rules, Angular Material/Zoneless Gotchas & Test Organisation Rules

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 12. TypeScript Rules in Tests

The project enforces `strict: true` — tests must compile cleanly:

- **No `any`** — type every mock object with the interface or `Partial<Interface>`.
- **No `!` non-null assertions** — use `as Type` casts only when the type is guaranteed.
- **Readonly inputs** — `@Input({ required: true })` fields must always be supplied in `componentProperties`.
- **Strict null checks** — optional properties in mocks must match the interface (include or explicitly omit).

Example of a correctly typed mock asset:

```typescript
const mockAsset: Asset = {
  assetId: 1,
  fileName: "photo.jpg",
  fileSize: 204800,
  thumbnailUrl: "/api/assets/1/thumbnail",
  folderPath: "/photos",
  imageRotation: "ROTATE_0",
  fileCreationDateTime: "2024-01-01T00:00:00",
  fileModificationDateTime: "2024-01-01T00:00:00",
  thumbnailCreationDateTime: "2024-01-01T00:00:00",
};
```

Use `Partial<AssetService>` for service stubs and only define the methods called by
the component under test.

---

## 13. Angular Material and zoneless-Angular gotchas

- Always pass `provideNoopAnimations()` — real animations cause flaky timing failures.
- Material overlay components (`MatSnackBar`, `MatDialog`, `MatMenu`) render in a portal
  outside the component root; query them with `cy.get('.mat-mdc-snack-bar-container')`.
- Use `MatSnackBar` snackbar detection:

```typescript
cy.get(".mat-mdc-snack-bar-label").should("contain", "Failed to load assets");
```

- **Mutating a plain (non-signal) component field directly from test code,
  then calling `fixture.detectChanges()`, is not guaranteed to re-render.**
  This app is zoneless (`provideZonelessChangeDetection()`), so a component
  is only rechecked when: a signal it reads changes, an `@Input()`/
  signal-`input()` binding changes, an event handler *in its own template*
  fires, or its `ChangeDetectorRef` is explicitly marked dirty. Calling a
  component method directly from test code that mutates a plain field read
  in the template does **not** go through any of those paths. Fix:
  ```typescript
  import { ChangeDetectorRef } from "@angular/core";

  cy.mount(SomeComponent).then(({ component, fixture }) => {
    component.someMethodThatMutatesAPlainField();
    fixture.componentRef.injector.get(ChangeDetectorRef).markForCheck();
    fixture.detectChanges();
  });
  ```
  This is *not* needed when the interaction goes through the DOM
  (`cy.get(...).click()` on a real template-bound event handler) — only
  when test code calls a component method directly and that method
  mutates a plain field rather than a signal.

---

## 14. Test Organisation Rules

- **One `describe` per class** — `describe('GalleryComponent', () => { ... })`.
- **Nested `describe` for groups of related behaviours** — e.g. `describe('pagination', () => { ... })`.
- **One behaviour per `it` block** — do not assert unrelated things in a single test.
- **`beforeEach` for shared setup** — extract repeated `mount()` calls into `beforeEach` when all
  tests in a `describe` share the same configuration.
- **`cy.stub()` reset** — stubs created with `cy.stub()` are automatically reset between tests.
- **No shared mutable component state** — never read `fixture.componentInstance` from one test and
  rely on it in the next.

---

