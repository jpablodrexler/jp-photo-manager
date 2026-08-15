---
name: cypress-unit-test-developer
description: >
  Cypress Component Testing skill for writing unit/component tests for the
  JPPhotoManager Angular 19 frontend. TRIGGER when creating or modifying
  *.cy.ts test files for standalone components, services, or pipes —
  including when an OpenSpec task calls for frontend tests. Always invoke
  alongside angular-developer when a new Angular component or service is
  created. Enforces the project's strict TypeScript conventions and
  feature-based architecture. Invoke this skill proactively — do not wait
  to be asked.
metadata:
  scope: [JPPhotoManagerWeb/frontend]
---

# Cypress Unit Test Developer Skill

Write Cypress Component Tests that follow the conventions and best practices of
the JPPhotoManager frontend: Angular 19 / TypeScript 5.6, standalone components,
Angular Material, RxJS, and strict TypeScript.

## Workflow

Make a todo list and work through it one task at a time.

---

## 1. Architecture (already set up — read this for context, not setup steps)

Cypress is a `JPPhotoManagerWeb/frontend/` devDependency; `cypress.config.ts`
(that directory's root) already configures both the `component` and `e2e`
blocks (the `e2e` block belongs to the `e2e-suite` skill, not this one).
Nothing below needs to be installed or scaffolded — it already exists.
Four things about the setup are worth understanding before writing a new
test, because they explain why the conventions in this skill exist:

1. **Bundler is `webpack`, not an esbuild/`application` bundler.** Cypress
   15's Angular Component Testing preset only supports
   `devServer: { framework: 'angular', bundler: 'webpack' }` — there is no
   esbuild equivalent yet, unlike the app's own production build
   (`@angular/build:application`). This is why `@angular-devkit/build-angular`
   is a devDependency even though the app itself never uses it for
   building/serving — it exists solely to satisfy this preset.
2. **`cy.mount` comes from plain `cypress/angular`, not
   `cypress/angular-zoneless`**, even though this app itself is zoneless
   (`app.config.ts` calls `provideZonelessChangeDetection()`).
   `cypress/support/component.ts` already wires the correct one up as the
   global `cy.mount` command — never import `mount` directly in a test
   file, just call `cy.mount(...)`.
3. **Code coverage is already wired**: `component.devServer.webpackConfig`
   adds a post-loader `babel-loader` rule applying `babel-plugin-istanbul`
   to every bundled `.ts`/`.js` file (excluding `*.cy.ts` specs and
   `node_modules`), and `setupNodeEvents` registers
   `@cypress/code-coverage/task`. `npm run test` runs the plain suite; `npm
   run test:coverage` (`cypress run --component --env coverage=true`) runs
   the same specs and additionally writes `html`/`lcov`/text-summary
   reports to `coverage/` (gitignored); `npm run coverage:check` (`nyc
   check-coverage`, reading `.nycrc.json`) enforces thresholds afterward. A
   scope under 80% is a 🟡 finding per `code-reviewer` skill §19.1 — add the
   missing test cases for the uncovered lines/branches the report lists,
   then re-run `coverage:check` to confirm it clears 80%.
4. **Trending snapshot**: `npm run coverage:trend-report`
   (`scripts/code-coverage-report.js`) re-runs the suite itself and writes
   a dated markdown report to `docs/reports/code-coverage/` — the
   project-wide percentages plus a table of every file still below 80% on
   any metric. Useful after a batch of new/expanded test files (like this
   skill produces) to get a written record of the before/after numbers,
   rather than re-deriving them from a raw `nyc report` run each time.

If you ever need to touch this setup (rare), it lives in
`JPPhotoManagerWeb/frontend/cypress.config.ts`,
`JPPhotoManagerWeb/frontend/cypress/support/component.ts`, and
`JPPhotoManagerWeb/frontend/cypress/tsconfig.json`. There is no dedicated
`tsconfig.cy.json` here — `tsconfig.app.json`
doesn't `exclude` `*.cy.ts` files in this project, so no override was ever
needed.

---

## 2. File Naming and Location

| Rule                                        | Example                                                 |
| ------------------------------------------- | ------------------------------------------------------- |
| Test file sits **next to** the source file  | `gallery.component.cy.ts` beside `gallery.component.ts` |
| File extension is always `.cy.ts`           | `file-size.pipe.cy.ts`                                  |
| Spec pattern configured as `src/**/*.cy.ts` | Matches all feature/shared/core subdirectories          |

---

## 3. Naming Conventions

| Element     | Convention                          | Example                                 |
| ----------- | ----------------------------------- | --------------------------------------- |
| Test suite  | `describe('<ClassName>')`           | `describe('GalleryComponent')`          |
| Test case   | `it('should <expected behaviour>')` | `it('should display asset thumbnails')` |
| Alias       | `cy.get(...).as('alias')`           | `.as('assetGrid')`                      |
| Stub object | `<ServiceName>Stub`                 | `assetServiceStub`                      |

---

## 4. Component Test Structure

### Minimal template

```typescript
import { mount } from "cypress/angular";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { ThumbnailComponent } from "./thumbnail.component";
import { Asset } from "../../core/models/asset.model";

describe("ThumbnailComponent", () => {
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

  it("should render the asset file name", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.contains(".thumbnail-name", "photo.jpg");
  });

  it("should apply selected class when selected input is true", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset, selected: true },
      providers: [provideNoopAnimations()],
    });

    cy.get("mat-card").should("have.class", "selected");
  });

  it("should hide broken image on error", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: {
        asset: { ...mockAsset, thumbnailUrl: "/bad-url" },
      },
      providers: [provideNoopAnimations()],
    });

    cy.get("img").then(($img) => {
      $img[0].dispatchEvent(new Event("error"));
    });
    cy.get("img").should("have.css", "display", "none");
  });
});
```

**Rules:**

- Always pass `provideNoopAnimations()` — Angular Material animations break Cypress otherwise.
- Use `componentProperties` (not `inputs`) — it maps directly to `@Input()` properties.
- Create typed mock objects that match the model interface exactly.

---

## 5. Mocking Services in Components

Provide a stub object with `cy.stub()` methods via the `providers` array:

```typescript
import { mount } from "cypress/angular";
import { of } from "rxjs";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { GalleryComponent } from "./gallery.component";
import { AssetService } from "../../core/services/asset.service";
import { PaginatedData } from "../../core/models/paginated-data.model";
import { Asset } from "../../core/models/asset.model";

describe("GalleryComponent", () => {
  const mockPage: PaginatedData<Asset> = {
    items: [
      {
        assetId: 1,
        fileName: "sunset.jpg",
        fileSize: 1024000,
        thumbnailUrl: "/api/assets/1/thumbnail",
        folderPath: "/photos",
        imageRotation: "ROTATE_0",
        fileCreationDateTime: "2024-06-01T10:00:00",
        fileModificationDateTime: "2024-06-01T10:00:00",
        thumbnailCreationDateTime: "2024-06-01T10:00:00",
      },
    ],
    pageIndex: 0,
    totalPages: 1,
    totalItems: 1,
  };

  function mountGallery(assetServiceOverrides: Partial<AssetService> = {}) {
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of(mockPage)),
      catalogAssets: cy.stub().returns(new MockEventSource()),
      deleteAssets: cy.stub().returns(of(undefined)),
      ...assetServiceOverrides,
    };

    return cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    });
  }

  it("should create the component", () => {
    mountGallery();
    cy.get("app-gallery").should("exist");
  });

  it("should display thumbnails after a folder is selected", () => {
    mountGallery();

    cy.get("app-folder-nav").then(($nav) => {
      $nav[0].dispatchEvent(
        new CustomEvent("folderSelected", { detail: "/photos", bubbles: true }),
      );
    });

    cy.get("app-thumbnail").should("have.length", 1);
  });
});
```

### MockEventSource helper

Declare a reusable mock for `EventSource` to avoid actual SSE connections:

```typescript
// cypress/support/mock-event-source.ts
export class MockEventSource implements Partial<EventSource> {
  private listeners: Record<string, EventListenerOrEventListenerObject[]> = {};

  readonly CONNECTING = 0 as const;
  readonly OPEN = 1 as const;
  readonly CLOSED = 2 as const;
  readyState: number = 1;
  url = "";
  withCredentials = false;
  onopen: ((this: EventSource, ev: Event) => unknown) | null = null;
  onmessage: ((this: EventSource, ev: MessageEvent) => unknown) | null = null;
  onerror: ((this: EventSource, ev: Event) => unknown) | null = null;

  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
  ): void {
    if (!this.listeners[type]) this.listeners[type] = [];
    this.listeners[type].push(listener);
  }

  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
  ): void {
    this.listeners[type] = (this.listeners[type] ?? []).filter(
      (l) => l !== listener,
    );
  }

  dispatchEvent(event: Event): boolean {
    (this.listeners[event.type] ?? []).forEach((l) => {
      if (typeof l === "function") l(event);
      else l.handleEvent(event);
    });
    return true;
  }

  emit(type: string, data: unknown): void {
    const event = new MessageEvent(type, { data: JSON.stringify(data) });
    this.dispatchEvent(event);
  }

  close(): void {
    this.readyState = 2;
  }
}
```

Import it in tests and in `cypress/support/component.ts`:

```typescript
// cypress/support/component.ts
export { MockEventSource } from "./mock-event-source";
```

---

## 6. Service Unit Tests (HTTP)

Test services by mounting a minimal host component that calls the service, or test
the service class directly using `TestBed` inside a Cypress test:

```typescript
import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { AssetService } from "./asset.service";
import { PaginatedData } from "../models/paginated-data.model";
import { Asset } from "../models/asset.model";

describe("AssetService", () => {
  let service: AssetService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AssetService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AssetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("should GET /api/assets with correct query params", () => {
    const mockData: PaginatedData<Asset> = {
      items: [],
      pageIndex: 0,
      totalPages: 0,
      totalItems: 0,
    };

    service.getAssets("/photos", 0, "FILE_NAME").subscribe((data) => {
      expect(data).to.deep.equal(mockData);
    });

    const req = httpMock.expectOne((r) => r.url === "/api/assets");
    expect(req.request.method).to.equal("GET");
    expect(req.request.params.get("folderPath")).to.equal("/photos");
    expect(req.request.params.get("page")).to.equal("0");
    expect(req.request.params.get("sort")).to.equal("FILE_NAME");
    req.flush(mockData);
  });

  it("should DELETE /api/assets with assetIds and deleteFiles params", () => {
    service.deleteAssets([1, 2], true).subscribe();

    const req = httpMock.expectOne((r) => r.url === "/api/assets");
    expect(req.request.method).to.equal("DELETE");
    expect(req.request.params.get("assetIds")).to.equal("1,2");
    expect(req.request.params.get("deleteFiles")).to.equal("true");
    req.flush(null);
  });

  it("should return a thumbnail URL string", () => {
    expect(service.getThumbnailUrl(42)).to.equal("/api/assets/42/thumbnail");
  });

  it("should return an EventSource for catalog SSE", () => {
    const source = service.catalogAssets();
    expect(source).to.be.instanceOf(EventSource);
    source.close();
  });
});
```

**Key rules:**

- Use `provideHttpClient()` + `provideHttpClientTesting()` (functional providers, Angular 19 style).
- Always call `httpMock.verify()` in `afterEach` to catch unexpected HTTP calls.
- Use Cypress assertions (`expect(...).to.equal(...)`) — not Jasmine (`toBe`).

---

## 7. Pipe Unit Tests

Test pipes directly as plain TypeScript classes — no component mounting needed:

```typescript
import { FileSizePipe } from "./file-size.pipe";

describe("FileSizePipe", () => {
  const pipe = new FileSizePipe();

  it('should return "0 B" for zero bytes', () => {
    expect(pipe.transform(0)).to.equal("0 B");
  });

  it("should format bytes", () => {
    expect(pipe.transform(512)).to.equal("512.0 B");
  });

  it("should format kilobytes", () => {
    expect(pipe.transform(1024)).to.equal("1.0 KB");
  });

  it("should format megabytes", () => {
    expect(pipe.transform(1048576)).to.equal("1.0 MB");
  });

  it("should format gigabytes", () => {
    expect(pipe.transform(1073741824)).to.equal("1.0 GB");
  });
});
```

---

## 8. Testing Angular Output Events

Use `cy.stub()` on a callback function and bind it as an output handler:

```typescript
import { mount } from "cypress/angular";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { FolderNavComponent } from "./folder-nav.component";
import { FolderService } from "../../core/services/folder.service";
import { of } from "rxjs";
import { Folder } from "../../core/models/folder.model";

describe("FolderNavComponent", () => {
  const mockFolders: Folder[] = [
    { folderId: 1, path: "/photos", name: "photos", hasChildren: false },
  ];

  it("should emit folderSelected when a folder node is clicked", () => {
    const folderServiceStub: Partial<FolderService> = {
      getInitialFolder: cy.stub().returns(of("/photos")),
      getDrives: cy.stub().returns(of(["C:"])),
      getFolders: cy.stub().returns(of(mockFolders)),
    };

    const onFolderSelected = cy.stub();

    cy.mount(FolderNavComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: FolderService, useValue: folderServiceStub },
      ],
    }).then(({ fixture }) => {
      fixture.componentInstance.folderSelected.subscribe(onFolderSelected);
    });

    cy.contains("photos").click();
    cy.wrap(onFolderSelected).should("have.been.calledWith", "/photos");
  });
});
```

---

## 9. Testing EventSource / SSE in Components

Use `MockEventSource` (from section 5) and trigger synthetic events:

```typescript
import { mount } from "cypress/angular";
import { of } from "rxjs";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { GalleryComponent } from "./gallery.component";
import { AssetService } from "../../core/services/asset.service";
import { MockEventSource } from "../../../cypress/support/mock-event-source";

describe("GalleryComponent — catalog SSE", () => {
  it("should update catalogProgress when a catalog event is received", () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy
        .stub()
        .returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
      catalogAssets: cy.stub().returns(mockSource),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    });

    cy.then(() => {
      mockSource.emit("catalog", {
        percentCompleted: 50,
        message: "Scanning…",
        reason: "SCANNING",
      });
    });

    cy.get("mat-progress-bar").should("have.attr", "aria-valuenow", "50");
  });

  it("should stop cataloging on SSE error", () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy
        .stub()
        .returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
      catalogAssets: cy.stub().returns(mockSource),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    }).then(({ fixture }) => {
      expect(fixture.componentInstance.cataloging).to.be.true;
    });

    cy.then(() => {
      mockSource.dispatchEvent(new Event("error"));
    });

    cy.then(
      ({ fixture }: { fixture: { componentInstance: GalleryComponent } }) => {
        expect(fixture.componentInstance.cataloging).to.be.false;
      },
    );
  });
});
```

---

## 10. DOM Interaction Patterns

| Action                           | Cypress code                                                                       |
| -------------------------------- | ---------------------------------------------------------------------------------- |
| Click a button                   | `cy.get('button[data-cy="next-page"]').click()`                                    |
| Type into an input               | `cy.get('input').type('search term')`                                              |
| Select a mat-select option       | `cy.get('mat-select').click(); cy.get('mat-option').contains('File Size').click()` |
| Assert text content              | `cy.contains('.thumbnail-name', 'photo.jpg')`                                      |
| Assert element count             | `cy.get('app-thumbnail').should('have.length', 3)`                                 |
| Assert class present             | `cy.get('mat-card').should('have.class', 'selected')`                              |
| Assert class absent              | `cy.get('mat-card').should('not.have.class', 'selected')`                          |
| Assert disabled state            | `cy.get('button').should('be.disabled')`                                           |
| Assert element hidden            | `cy.get('.spinner').should('not.exist')`                                           |
| Trigger Angular change detection | `cy.then(({ fixture }) => fixture.detectChanges())`                                |

Add `data-cy` attributes to templates only when a stable query selector is needed
and there is no other semantic selector available:

```html
<button mat-icon-button data-cy="prev-page" (click)="prevPage()"></button>
```

---

## 11. Assertions Reference

Always use Cypress's Chai-based assertions, **not** Jasmine matchers:

| Assertion        | Cypress (Chai)                                      |
| ---------------- | --------------------------------------------------- |
| Equality         | `expect(val).to.equal('text')`                      |
| Deep equality    | `expect(obj).to.deep.equal({ a: 1 })`               |
| Truthiness       | `expect(val).to.be.true`                            |
| Existence        | `expect(val).to.exist`                              |
| Array length     | `expect(arr).to.have.length(3)`                     |
| Include          | `expect(str).to.include('substring')`               |
| Stub called      | `cy.wrap(stub).should('have.been.called')`          |
| Stub called with | `cy.wrap(stub).should('have.been.calledWith', arg)` |
| Stub call count  | `cy.wrap(stub).should('have.been.calledOnce')`      |

**Testing that a `Promise`-returning method rejects**: never chain
`.then(onFulfilled, onRejected)` directly on `cy.wrap(aRejectingPromise)`
— `cy.wrap()` fails the test the instant the wrapped promise rejects,
before the second callback ever runs. Pre-resolve the rejection into a
plain value first, then wrap *that*:

```typescript
function rejectionOf<T>(promise: Promise<T>): Promise<unknown> {
  return promise.then(
    () => {
      throw new Error("expected the promise to reject, but it resolved");
    },
    (err) => err,
  );
}

cy.wrap(rejectionOf(someMethodThatReturnsAPromise())).should(
  "deep.equal",
  expectedError,
);
```

Most of this app's own service methods return `Observable`s (via
`HttpClient`), not `Promise`s — assert an `Observable`'s error path the
normal way instead, via `req.flush(errorBody, { status, statusText })` and
an `error` callback on `.subscribe(...)`. This pattern only matters for
the minority of methods that genuinely return a `Promise` (e.g. a plain
async utility function, or an `EventSource`-adjacent helper).

---

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

## 15. Running Tests

```bash
# Open Cypress Test Runner (interactive)
npm run cypress:open

# Run headlessly (CI)
npm run cypress:run

# Run a single spec
npx cypress run --component --spec "src/app/features/gallery/gallery.component.cy.ts"

# Check line coverage against the 80% minimum (see code-reviewer skill §19.1)
npm run test:coverage
npm run coverage:check
```

---

## Wrap Up

After creating or modifying test files, provide a brief summary covering:

1. Test files created or modified and what they cover
2. Any new `data-cy` attributes added to templates
3. Whether `MockEventSource` was used and where it lives
4. How to run the new tests:
   ```bash
   npx cypress run --component --spec "src/app/<path>/<file>.cy.ts"
   ```
5. Any gotcha from §13 (zoneless `markForCheck()`) or §11 (`rejectionOf()`)
   that a new test needed
