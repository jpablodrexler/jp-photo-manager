# Angular Developer — Custom Pipes & Testing (Cypress Component Testing)

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 15. Custom Pipes

```typescript
@Pipe({ name: "fileSize", standalone: true })
export class FileSizePipe implements PipeTransform {
  transform(bytes: number): string {
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
  }
}
```

All pipes must be `standalone: true` and declared in the component's `imports: []`.

---

## 16. Testing (Cypress Component Testing)

Unit/component tests run via Cypress Component Testing — real browser
rendering (Electron, headless by default), not Karma/Jasmine or jsdom.
`describe`/`it` are Mocha globals, `expect` is Chai's, and stubbing goes
through `cy.stub()` rather than `jasmine.createSpyObj`. Full conventions,
worked examples, and the two most important gotchas discovered building
this suite (zoneless `markForCheck()`, and why
`cy.wrap(rejectingPromise).then(...)` doesn't work) live in the dedicated
**`cypress-unit-test-developer`** skill — read that before writing a new
test. The short version:

```typescript
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { GalleryComponent } from "./gallery.component";
import { AssetService } from "../../core/services/asset.service";
import { of } from "rxjs";

describe("GalleryComponent", () => {
  function mountGallery(assetServiceOverrides: Partial<AssetService> = {}) {
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy
        .stub()
        .returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
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

  it("should create", () => {
    mountGallery();
    cy.get("app-gallery").should("exist");
  });

  it("should call getAssets on init", () => {
    const getAssets = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 }));
    mountGallery({ getAssets });
    cy.wrap(getAssets).should("have.been.called");
  });
});
```

**Testing rules:**

- `cy.mount(StandaloneComponent, { providers: [...] })` — no module setup,
  and `cy.mount` is a global command (registered in
  `cypress/support/component.ts`); never import `mount` directly.
- Stub services with a `Partial<ServiceType>` object using `cy.stub()` per
  method (as above), not `jasmine.createSpyObj` or a hand-written class.
- Always include `provideNoopAnimations()`. Provide `provideRouter([])`
  whenever the component injects `Router` or uses `RouterLink`.
- Assertions like `cy.get(...).should(...)`/`cy.contains(...)` auto-retry
  until they pass or time out — no manual microtask-flushing needed for a
  component whose `ngOnInit` does async work. Reach for
  `fixture.detectChanges()` (plus `ChangeDetectorRef.markForCheck()` if
  mutating a plain field directly from test code — see
  `cypress-unit-test-developer` §13) only when driving the component
  through its instance/fixture directly rather than through the DOM.
- One behaviour per `it` block; name tests `it('should <expected
  behaviour>', ...)`.
- Co-locate spec files: `gallery.component.cy.ts` next to
  `gallery.component.ts`.

### Service Tests

```typescript
import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting, HttpTestingController } from "@angular/common/http/testing";
import { AssetService } from "./asset.service";
import { PaginatedData } from "../models/paginated-data.model";
import { Asset } from "../models/asset.model";

describe("AssetService", () => {
  let service: AssetService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AssetService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AssetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("should fetch assets", () => {
    const mockData: PaginatedData<Asset> = {
      items: [],
      pageIndex: 0,
      totalPages: 0,
      totalItems: 0,
    };
    service.getAssets("/photos", 0, "FILE_NAME").subscribe((data) => {
      expect(data).to.deep.equal(mockData);
    });
    const req = httpMock.expectOne((r) => r.url.includes("/api/assets"));
    expect(req.request.method).to.equal("GET");
    req.flush(mockData);
  });
});
```

This project's services are `HttpClient`/`Observable`-based throughout, so
`HttpTestingController` is the normal way to test them — see
`cypress-unit-test-developer` §6 for further worked examples. The
`rejectionOf()` helper in that skill's §11 only matters for the minority of
methods that genuinely return a `Promise` rather than an `Observable`.

---

