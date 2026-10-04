# Code Reviewer — Frontend: TypeScript, Naming & Cypress Tests

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 11. Frontend: TypeScript Conventions

🔴 Flag any use of `any` — use a typed interface, `unknown` with a type
guard, or `Partial<T>`.

🔴 Flag `console.log` / `console.error` left in committed code — use
`MatSnackBar` for user-facing feedback and remove debug output.

🟡 Flag `!` (non-null assertion) that could be replaced with optional
chaining (`?.`) or a null check.

🟡 Flag a property initialised as `undefined` that could be typed as optional
(`field?: Type`).

🟡 Flag an interface defined inline in a component — move it to
`core/models/`.

---

## 12. Frontend: Naming Conventions

| Element            | Expected                   | Example                                    |
| ------------------ | -------------------------- | ------------------------------------------ |
| File               | `kebab-case.<type>.ts`     | `asset.service.ts`, `gallery.component.ts` |
| Class              | PascalCase                 | `GalleryComponent`, `AssetService`         |
| Interface          | PascalCase                 | `Asset`, `PaginatedData`                   |
| String union       | UPPER_SNAKE_CASE           | `'FILE_NAME' \| 'FILE_SIZE'`               |
| Property / method  | camelCase                  | `currentFolder`, `loadAssets()`            |
| Component selector | `app-kebab-case`           | `app-gallery`                              |
| Test suite         | `describe('ClassName')`    | `describe('GalleryComponent')`             |
| Test case          | `it('should <behaviour>')` | `it('should display thumbnails')`          |

🟡 Flag any violation of the above.

---

## 13. Frontend: Cypress Tests

🔴 Flag Cypress tests that use Jasmine matchers (`toBe`, `toEqual`) — use
Chai assertions (`expect(...).to.equal(...)`, `cy.get(...).should(...)`).

🔴 Flag component tests missing `provideNoopAnimations()` — Angular Material
animations cause flaky failures in Cypress.

🟡 Flag service stubs typed as `any` — use `Partial<ServiceType>`.

🟡 Flag `EventSource` usage in a test without `MockEventSource` — real SSE
connections must not be opened in component tests.

🟡 Flag test files placed outside the source tree (e.g., in a top-level
`tests/` folder) — test files must be co-located with their source files as
`*.cy.ts`.

🟡 Flag a test that mutates a component's plain (non-signal) field directly
(e.g. calling a component method from test code that sets a field, then
asserting on the resulting DOM state) via only `fixture.detectChanges()`
— in this zoneless app that needs an explicit
`fixture.componentRef.injector.get(ChangeDetectorRef).markForCheck()`
first, or the view is never rechecked.

🟡 Flag `import { mount } from 'cypress/angular'` in a test file — `cy.mount`
is a global command already registered in `cypress/support/component.ts`;
a test file should never import `mount` directly.

🟡 Flag a `describe` block with no `beforeEach` that repeats the same
`cy.mount()` call in every `it` — extract to `beforeEach`.

🟡 Flag a CSS/template fix, or a new multi-field/multi-button UI, with no
layout regression test in the component's `.cy.ts` — a `cy.viewport(384, 824)`
**and** a desktop-width `cy.viewport(...)` assertion on measured geometry
(`getBoundingClientRect`: no overlap, fields filling their container, buttons
sharing a width once wrapped). A layout test that only checks the element
exists passes while the real layout is broken. Say the layout is
**unverified** rather than approving it when the PR or report doesn't state
it was checked at both widths.

---
