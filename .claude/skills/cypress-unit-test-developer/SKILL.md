---
name: cypress-unit-test-developer
description: >
  Cypress Component Testing skill for writing unit/component tests for the
  JPPhotoManager Angular 19 frontend. Use when creating or modifying *.cy.ts
  test files for standalone components, services, or pipes following the
  project's strict TypeScript conventions and feature-based architecture.
---

# Cypress Unit Test Developer Skill

Write Cypress Component Tests that follow the conventions and best practices of
the JPPhotoManager frontend: Angular 19 / TypeScript 5.6, standalone components,
Angular Material, RxJS, and strict TypeScript.

## Workflow

Make a todo list and work through it one task at a time.

---

## 1. Project Setup

### Install Cypress for Angular Component Testing

```bash
cd JPPhotoManagerWeb/frontend
npm install --save-dev cypress @cypress/schematic
npx cypress open --component   # follow the wizard to configure Angular
```

Cypress will scaffold `cypress.config.ts` and `cypress/support/`.

### cypress.config.ts

```typescript
import { defineConfig } from 'cypress';

export default defineConfig({
  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack',
    },
    specPattern: 'src/**/*.cy.ts',
    supportFile: 'cypress/support/component.ts',
  },
});
```

### cypress/support/component.ts

```typescript
import { mount } from 'cypress/angular';

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount;
    }
  }
}

Cypress.Commands.add('mount', mount);
```

### tsconfig.json — add cypress types

```jsonc
{
  "compilerOptions": {
    // existing options remain unchanged
  },
  "exclude": ["node_modules", "cypress"]  // keep cypress types isolated
}
```

Create `cypress/tsconfig.json` for the test files:

```jsonc
{
  "extends": "../tsconfig.json",
  "compilerOptions": {
    "types": ["cypress"],
    "isolatedModules": false
  },
  "include": ["**/*.ts"]
}
```

### npm scripts

Add to `package.json`:

```json
{
  "scripts": {
    "cypress:open": "cypress open --component",
    "cypress:run":  "cypress run --component"
  }
}
```

---

## 2. File Naming and Location

| Rule | Example |
|---|---|
| Test file sits **next to** the source file | `gallery.component.cy.ts` beside `gallery.component.ts` |
| File extension is always `.cy.ts` | `file-size.pipe.cy.ts` |
| Spec pattern configured as `src/**/*.cy.ts` | Matches all feature/shared/core subdirectories |

---

## 3. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Test suite | `describe('<ClassName>')` | `describe('GalleryComponent')` |
| Test case | `it('should <expected behaviour>')` | `it('should display asset thumbnails')` |
| Alias | `cy.get(...).as('alias')` | `.as('assetGrid')` |
| Stub object | `<ServiceName>Stub` | `assetServiceStub` |

---

## 4. Component Test Structure

### Minimal template

```typescript
import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ThumbnailComponent } from './thumbnail.component';
import { Asset } from '../../core/models/asset.model';

describe('ThumbnailComponent', () => {
  const mockAsset: Asset = {
    assetId: 1,
    fileName: 'photo.jpg',
    fileSize: 204800,
    thumbnailUrl: '/api/assets/1/thumbnail',
    folderPath: '/photos',
    imageRotation: 'ROTATE_0',
    fileCreationDateTime: '2024-01-01T00:00:00',
    fileModificationDateTime: '2024-01-01T00:00:00',
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
  };

  it('should render the asset file name', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.contains('.thumbnail-name', 'photo.jpg');
  });

  it('should apply selected class when selected input is true', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset, selected: true },
      providers: [provideNoopAnimations()],
    });

    cy.get('mat-card').should('have.class', 'selected');
  });

  it('should hide broken image on error', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: { ...mockAsset, thumbnailUrl: '/bad-url' } },
      providers: [provideNoopAnimations()],
    });

    cy.get('img').then($img => {
      $img[0].dispatchEvent(new Event('error'));
    });
    cy.get('img').should('have.css', 'display', 'none');
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
import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { GalleryComponent } from './gallery.component';
import { AssetService } from '../../core/services/asset.service';
import { PaginatedData } from '../../core/models/paginated-data.model';
import { Asset } from '../../core/models/asset.model';

describe('GalleryComponent', () => {
  const mockPage: PaginatedData<Asset> = {
    items: [
      {
        assetId: 1,
        fileName: 'sunset.jpg',
        fileSize: 1024000,
        thumbnailUrl: '/api/assets/1/thumbnail',
        folderPath: '/photos',
        imageRotation: 'ROTATE_0',
        fileCreationDateTime: '2024-06-01T10:00:00',
        fileModificationDateTime: '2024-06-01T10:00:00',
        thumbnailCreationDateTime: '2024-06-01T10:00:00',
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

  it('should create the component', () => {
    mountGallery();
    cy.get('app-gallery').should('exist');
  });

  it('should display thumbnails after a folder is selected', () => {
    mountGallery();

    cy.get('app-folder-nav').then($nav => {
      $nav[0].dispatchEvent(
        new CustomEvent('folderSelected', { detail: '/photos', bubbles: true })
      );
    });

    cy.get('app-thumbnail').should('have.length', 1);
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
  url = '';
  withCredentials = false;
  onopen: ((this: EventSource, ev: Event) => unknown) | null = null;
  onmessage: ((this: EventSource, ev: MessageEvent) => unknown) | null = null;
  onerror: ((this: EventSource, ev: Event) => unknown) | null = null;

  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    if (!this.listeners[type]) this.listeners[type] = [];
    this.listeners[type].push(listener);
  }

  removeEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    this.listeners[type] = (this.listeners[type] ?? []).filter(l => l !== listener);
  }

  dispatchEvent(event: Event): boolean {
    (this.listeners[event.type] ?? []).forEach(l => {
      if (typeof l === 'function') l(event);
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
export { MockEventSource } from './mock-event-source';
```

---

## 6. Service Unit Tests (HTTP)

Test services by mounting a minimal host component that calls the service, or test
the service class directly using `TestBed` inside a Cypress test:

```typescript
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AssetService } from './asset.service';
import { PaginatedData } from '../models/paginated-data.model';
import { Asset } from '../models/asset.model';

describe('AssetService', () => {
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

  it('should GET /api/assets with correct query params', () => {
    const mockData: PaginatedData<Asset> = {
      items: [],
      pageIndex: 0,
      totalPages: 0,
      totalItems: 0,
    };

    service.getAssets('/photos', 0, 'FILE_NAME').subscribe(data => {
      expect(data).to.deep.equal(mockData);
    });

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('folderPath')).to.equal('/photos');
    expect(req.request.params.get('page')).to.equal('0');
    expect(req.request.params.get('sort')).to.equal('FILE_NAME');
    req.flush(mockData);
  });

  it('should DELETE /api/assets with assetIds and deleteFiles params', () => {
    service.deleteAssets([1, 2], true).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.method).to.equal('DELETE');
    expect(req.request.params.get('assetIds')).to.equal('1,2');
    expect(req.request.params.get('deleteFiles')).to.equal('true');
    req.flush(null);
  });

  it('should return a thumbnail URL string', () => {
    expect(service.getThumbnailUrl(42)).to.equal('/api/assets/42/thumbnail');
  });

  it('should return an EventSource for catalog SSE', () => {
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
import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
  const pipe = new FileSizePipe();

  it('should return "0 B" for zero bytes', () => {
    expect(pipe.transform(0)).to.equal('0 B');
  });

  it('should format bytes', () => {
    expect(pipe.transform(512)).to.equal('512.0 B');
  });

  it('should format kilobytes', () => {
    expect(pipe.transform(1024)).to.equal('1.0 KB');
  });

  it('should format megabytes', () => {
    expect(pipe.transform(1048576)).to.equal('1.0 MB');
  });

  it('should format gigabytes', () => {
    expect(pipe.transform(1073741824)).to.equal('1.0 GB');
  });
});
```

---

## 8. Testing Angular Output Events

Use `cy.stub()` on a callback function and bind it as an output handler:

```typescript
import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { FolderNavComponent } from './folder-nav.component';
import { FolderService } from '../../core/services/folder.service';
import { of } from 'rxjs';
import { Folder } from '../../core/models/folder.model';

describe('FolderNavComponent', () => {
  const mockFolders: Folder[] = [
    { folderId: 1, path: '/photos', name: 'photos', hasChildren: false },
  ];

  it('should emit folderSelected when a folder node is clicked', () => {
    const folderServiceStub: Partial<FolderService> = {
      getInitialFolder: cy.stub().returns(of('/photos')),
      getDrives: cy.stub().returns(of(['C:'])),
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

    cy.contains('photos').click();
    cy.wrap(onFolderSelected).should('have.been.calledWith', '/photos');
  });
});
```

---

## 9. Testing EventSource / SSE in Components

Use `MockEventSource` (from section 5) and trigger synthetic events:

```typescript
import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { GalleryComponent } from './gallery.component';
import { AssetService } from '../../core/services/asset.service';
import { MockEventSource } from '../../../cypress/support/mock-event-source';

describe('GalleryComponent — catalog SSE', () => {
  it('should update catalogProgress when a catalog event is received', () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
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
      mockSource.emit('catalog', { percentCompleted: 50, message: 'Scanning…', reason: 'SCANNING' });
    });

    cy.get('mat-progress-bar').should('have.attr', 'aria-valuenow', '50');
  });

  it('should stop cataloging on SSE error', () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
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
      mockSource.dispatchEvent(new Event('error'));
    });

    cy.then(({ fixture }: { fixture: { componentInstance: GalleryComponent } }) => {
      expect(fixture.componentInstance.cataloging).to.be.false;
    });
  });
});
```

---

## 10. DOM Interaction Patterns

| Action | Cypress code |
|---|---|
| Click a button | `cy.get('button[data-cy="next-page"]').click()` |
| Type into an input | `cy.get('input').type('search term')` |
| Select a mat-select option | `cy.get('mat-select').click(); cy.get('mat-option').contains('File Size').click()` |
| Assert text content | `cy.contains('.thumbnail-name', 'photo.jpg')` |
| Assert element count | `cy.get('app-thumbnail').should('have.length', 3)` |
| Assert class present | `cy.get('mat-card').should('have.class', 'selected')` |
| Assert class absent | `cy.get('mat-card').should('not.have.class', 'selected')` |
| Assert disabled state | `cy.get('button').should('be.disabled')` |
| Assert element hidden | `cy.get('.spinner').should('not.exist')` |
| Trigger Angular change detection | `cy.then(({ fixture }) => fixture.detectChanges())` |

Add `data-cy` attributes to templates only when a stable query selector is needed
and there is no other semantic selector available:

```html
<button mat-icon-button data-cy="prev-page" (click)="prevPage()">
```

---

## 11. Assertions Reference

Always use Cypress's Chai-based assertions, **not** Jasmine matchers:

| Assertion | Cypress (Chai) |
|---|---|
| Equality | `expect(val).to.equal('text')` |
| Deep equality | `expect(obj).to.deep.equal({ a: 1 })` |
| Truthiness | `expect(val).to.be.true` |
| Existence | `expect(val).to.exist` |
| Array length | `expect(arr).to.have.length(3)` |
| Include | `expect(str).to.include('substring')` |
| Stub called | `cy.wrap(stub).should('have.been.called')` |
| Stub called with | `cy.wrap(stub).should('have.been.calledWith', arg)` |
| Stub call count | `cy.wrap(stub).should('have.been.calledOnce')` |

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
  fileName: 'photo.jpg',
  fileSize: 204800,
  thumbnailUrl: '/api/assets/1/thumbnail',
  folderPath: '/photos',
  imageRotation: 'ROTATE_0',
  fileCreationDateTime: '2024-01-01T00:00:00',
  fileModificationDateTime: '2024-01-01T00:00:00',
  thumbnailCreationDateTime: '2024-01-01T00:00:00',
};
```

Use `Partial<AssetService>` for service stubs and only define the methods called by
the component under test.

---

## 13. Angular Material in Tests

- Always pass `provideNoopAnimations()` — real animations cause flaky timing failures.
- Material overlay components (`MatSnackBar`, `MatDialog`, `MatMenu`) render in a portal
  outside the component root; query them with `cy.get('.mat-mdc-snack-bar-container')`.
- Use `MatSnackBar` snackbar detection:

```typescript
cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load assets');
```

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
5. Any setup step still needed (e.g. first-time `cypress open` wizard for Angular devServer config)
