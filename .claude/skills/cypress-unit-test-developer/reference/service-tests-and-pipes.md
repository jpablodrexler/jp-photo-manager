# Cypress Unit Test Developer — Service Unit Tests (HTTP) & Pipe Unit Tests

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

