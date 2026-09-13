---
name: angular-developer
description: >
  Angular developer skill for writing Angular 19 applications following the
  JPPhotoManager frontend code style. TRIGGER whenever work touches
  JPPhotoManagerWeb/frontend — including when implementing OpenSpec tasks:
  creating or modifying components, services, models, pipes, or routes in an
  Angular project with a feature-based architecture of
  core → features ← shared. Invoke this skill proactively — do not wait to
  be asked.
metadata:
  scope: [JPPhotoManagerWeb/frontend]
---

# Angular Developer Skill

Write Angular code that follows the conventions and best practices of the
JPPhotoManager frontend project: an Angular 19 / TypeScript 5.6 application with
standalone components, Angular Material, RxJS, and a feature-based clean
architecture.

## Workflow

Make a todo list and work through it one task at a time.

---

## 1. Project Setup

### Build System

Use the **Angular CLI** (`@angular/cli`) with this core stack:

| Package             | Version   | Purpose                                 |
| ------------------- | --------- | --------------------------------------- |
| `@angular/core`     | `^19.0.0` | Framework core                          |
| `@angular/material` | `^19.0.0` | Material Design UI components           |
| `@angular/cdk`      | `^19.0.0` | Component Dev Kit (tree, overlay, etc.) |
| `@angular/forms`    | `^19.0.0` | Reactive & template-driven forms        |
| `@angular/router`   | `^19.0.0` | Client-side routing                     |
| `rxjs`              | `~7.8.0`  | Reactive programming                    |
| `typescript`        | `~5.6.0`  | Language                                |

**Dev dependencies:**

| Package                        | Purpose                                                            |
| ------------------------------- | -------------------------------------------------------------------- |
| `cypress`                       | Test runner — both component tests (§16) and the E2E suite          |
| `@angular-devkit/build-angular` | Only for Cypress's webpack-based Angular Component Testing preset — the app itself never uses it for building/serving (see §16) |

There is no Karma/Jasmine in this project — Cypress is the sole test
runner, for both the component/unit layer and the E2E layer. See §16
below and the `cypress-unit-test-developer`/`e2e-suite` skills.

**Key npm scripts:**

```bash
ng serve                         # Dev server (http://localhost:4200)
ng build                         # Development build
ng build --configuration production  # Production build
npm run test                     # cypress run --component — the unit/component test suite
npm run test:e2e                 # cypress run --e2e — the maintained E2E suite (backend must already be running)
ng lint                          # Lint the project
```

To run a single test file: `npx cypress run --component --spec
'src/app/features/gallery/gallery.component.cy.ts'`.

### Dev Proxy

Configure the dev proxy in `proxy.conf.json` to forward API calls to the backend:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 2. TypeScript Configuration

**tsconfig.json (base — never relax these):**

```jsonc
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "strict": true,
    "noImplicitOverride": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "forceConsistentCasingInFileNames": true,
    "moduleResolution": "bundler",
    "baseUrl": "./",
  },
  "angularCompilerOptions": {
    "strictInjectionParameters": true,
    "strictInputAccessModifiers": true,
    "strictTemplates": true,
  },
}
```

All strict flags **must remain enabled**. Never use `any`; prefer typed interfaces or `unknown`.

---

## 3. Directory Structure

```
src/
  main.ts                  # Bootstrap entry: bootstrapApplication(AppComponent, appConfig)
  index.html
  styles.scss              # Global styles + utility classes
  app/
    app.config.ts          # provideRouter, provideHttpClient, provideAnimations
    app.routes.ts          # Top-level route definitions
    app.component.ts       # Root shell (navigation bar)
    app.component.html
    app.component.scss
    app.component.cy.ts
    core/
      services/            # Application-wide singleton services
      models/              # TypeScript interfaces and type aliases
    features/
      <feature>/           # One folder per page/feature (lazy-loaded)
        <feature>.component.ts
        <feature>.component.html
        <feature>.component.scss
        <feature>.component.cy.ts
    shared/
      components/          # Reusable UI components (e.g. thumbnail)
      pipes/               # Custom Angular pipes
```

**Layer rules:**

- `core/` — services and models only; no UI.
- `features/` — page-level smart components; use core services and shared components.
- `shared/` — pure presentational components and pipes; no service calls.

---

## 4. Naming Conventions

| Element              | Convention                         | Example                                            |
| -------------------- | ---------------------------------- | -------------------------------------------------- |
| Files                | `kebab-case.<type>.ts`             | `asset.service.ts`, `gallery.component.ts`         |
| Classes              | PascalCase                         | `GalleryComponent`, `AssetService`, `FileSizePipe` |
| Interfaces           | PascalCase                         | `Asset`, `Folder`, `PaginatedData`                 |
| Type aliases         | PascalCase                         | `ViewMode`, `SortCriteria`                         |
| String union values  | UPPER_SNAKE_CASE                   | `'FILE_NAME' \| 'FILE_SIZE' \| 'FILE_DATE'`        |
| Properties & methods | camelCase                          | `currentFolder`, `loadAssets()`                    |
| Private fields       | camelCase (no underscore prefix)   | `private baseUrl = '/api/assets'`                  |
| Readonly constants   | camelCase or `readonly` property   | `private readonly baseUrl`                         |
| Component selectors  | `app-kebab-case`                   | `selector: 'app-gallery'`                          |
| Test classes         | `describe('<ClassName>')`          | `describe('GalleryComponent')`                     |
| Test methods         | `it('should <expected behavior>')` | `it('should display thumbnails')`                  |

---

## 5. Standalone Components

**All components must be standalone** — no NgModule files.

```typescript
@Component({
  selector: "app-gallery",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    // ... other Material modules and shared components
  ],
  templateUrl: "./gallery.component.html",
  styleUrl: "./gallery.component.scss",
})
export class GalleryComponent implements OnInit, OnDestroy {
  // component logic
}
```

- Declare every import the template uses directly in `imports: []`.
- Use `styleUrl` (singular) for a single stylesheet.
- Implement `OnDestroy` whenever you subscribe to observables or open `EventSource`.

---

## 6. Routing

Define routes in `app.routes.ts` using **lazy-loaded standalone components**:

```typescript
export const routes: Routes = [
  { path: "", redirectTo: "gallery", pathMatch: "full" },
  {
    path: "gallery",
    loadComponent: () =>
      import("./features/gallery/gallery.component").then(
        (m) => m.GalleryComponent,
      ),
  },
  {
    path: "sync",
    loadComponent: () =>
      import("./features/sync/sync.component").then((m) => m.SyncComponent),
  },
];
```

Bootstrap routing in `app.config.ts`:

```typescript
export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(), provideAnimations()],
};
```

---

## 7. Services

```typescript
@Injectable({ providedIn: "root" })
export class AssetService {
  private readonly baseUrl = "/api/assets";

  constructor(private http: HttpClient) {}

  getAssets(
    folderPath: string,
    pageIndex: number,
    sortCriteria: SortCriteria,
  ): Observable<PaginatedData<Asset>> {
    const params = { folderPath, pageIndex: String(pageIndex), sortCriteria };
    return this.http.get<PaginatedData<Asset>>(this.baseUrl, { params });
  }

  deleteAssets(assetIds: number[]): Observable<void> {
    return this.http.delete<void>(this.baseUrl, { body: assetIds });
  }

  catalogAssets(): EventSource {
    return new EventSource(`${this.baseUrl}/catalog`);
  }
}
```

**Service rules:**

- Always use `providedIn: 'root'` for tree-shaking.
- Return `Observable<T>` from all HTTP methods; never subscribe inside a service.
- Use `private readonly` for `baseUrl` and other constants.
- For Server-Sent Events, return the raw `EventSource` to the component.
- Never create a service (or file) whose entire content is a re-export/alias
  of another service under a different name
  (`export { FooService as BarService } from './foo.service';`), and never
  create a service class whose every method is a one-line passthrough to an
  injected service for the same capability. If a component needs `FooService`
  under a more domain-appropriate name, inject `FooService` directly — don't
  wrap it. This happened for real: `core/services/audio-player.service.ts`
  was a bare re-export of `MediaPlayerService` with zero importers anywhere in
  the codebase (`AudioPlayerComponent` already injected `MediaPlayerService`
  directly) — see the `code-reviewer` skill §15 for the incident. If you find
  one, delete it and repoint any real importers to the underlying service.

---

## 8. Models

Define all data shapes as TypeScript **interfaces** in `core/models/`:

```typescript
// asset.model.ts
export interface Asset {
  assetId: number;
  fileName: string;
  fileSize: number;
  thumbnailUrl: string;
  creationDateTime: string;
}

// paginated-data.model.ts
export interface PaginatedData<T> {
  items: T[];
  pageIndex: number;
  totalPages: number;
  totalItems: number;
}
```

Use **string union types** for enumerations:

```typescript
export type SortCriteria =
  | "FILE_NAME"
  | "FILE_SIZE"
  | "FILE_DATE"
  | "FILE_EXTENSION";
export type ViewMode = "thumbnails" | "viewer";
```

---

## 9. Templates

Use **Angular 17+ built-in control flow** — never `*ngIf` or `*ngFor`:

```html
<!-- Conditional rendering -->
@if (isLoading) {
<mat-spinner></mat-spinner>
} @else if (assets.length === 0) {
<p>No assets found.</p>
} @else {
<div class="thumbnail-grid">
  @for (asset of assets; track asset.assetId) {
  <app-thumbnail [asset]="asset" (selected)="onSelect(asset)" />
  }
</div>
}

<!-- Event binding -->
<button mat-button (click)="loadAssets()">Refresh</button>

<!-- Two-way binding -->
<mat-select [(ngModel)]="sortCriteria" (ngModelChange)="onSortChange()">
  @for (option of sortOptions; track option.value) {
  <mat-option [value]="option.value">{{ option.label }}</mat-option>
  }
</mat-select>

<!-- Property binding -->
<img [src]="asset.thumbnailUrl" [alt]="asset.fileName" />
```

---

## 10. Component State & RxJS

Components own their local UI state as plain class properties. There is no global state library.

```typescript
export class GalleryComponent implements OnInit, OnDestroy {
  assets: Asset[] = [];
  selectedAssets = new Set<number>();
  viewMode: ViewMode = "thumbnails";
  pageIndex = 0;
  totalPages = 0;
  totalItems = 0;
  isLoading = false;
  catalogProgress = 0;

  private catalogEventSource?: EventSource;

  constructor(
    private assetService: AssetService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadAssets();
  }

  ngOnDestroy(): void {
    this.catalogEventSource?.close();
  }

  loadAssets(): void {
    this.isLoading = true;
    this.assetService
      .getAssets(this.currentFolder, this.pageIndex, this.sortCriteria)
      .subscribe({
        next: (data) => {
          this.assets = data.items;
          this.totalPages = data.totalPages;
          this.totalItems = data.totalItems;
          this.isLoading = false;
        },
        error: () => {
          this.snackBar.open("Failed to load assets", "Dismiss", {
            duration: 3000,
          });
          this.isLoading = false;
        },
      });
  }
}
```

**State rules:**

- Store UI state as component properties; never introduce NgRx unless explicitly required.
- Subscribe in lifecycle hooks (`ngOnInit`); unsubscribe or complete in `ngOnDestroy`.
- Use `Set<T>` for selection tracking to get O(1) add/delete/has.
- Show user feedback with `MatSnackBar`.

---

## 11. Server-Sent Events (EventSource)

For long-running operations that stream progress back to the UI:

```typescript
startCatalog(): void {
  this.catalogEventSource = this.assetService.catalogAssets();

  this.catalogEventSource.addEventListener('catalog', (event: MessageEvent) => {
    const notification = JSON.parse(event.data);
    this.catalogProgress = notification.progress;
  });

  this.catalogEventSource.addEventListener('error', () => {
    this.snackBar.open('Catalog failed', 'Dismiss', { duration: 3000 });
    this.catalogEventSource?.close();
  });
}

stopCatalog(): void {
  this.catalogEventSource?.close();
  this.catalogEventSource = undefined;
}
```

Always close the `EventSource` in `ngOnDestroy` to prevent memory leaks.

**Authentication with EventSource:** The browser's `EventSource` API does not support custom request headers. Never pass a JWT as a query parameter (`?token=...`) — tokens in URLs appear in server logs and browser history. Use HttpOnly cookies instead (see section 19); the browser sends them automatically with same-origin EventSource connections.

---

## 12. Angular Material Usage

Import only the specific Material module(s) you need in each component's `imports: []`:

```typescript
imports: [
  MatToolbarModule, // <mat-toolbar>
  MatButtonModule, // mat-button, mat-icon-button, mat-fab
  MatIconModule, // <mat-icon>
  MatCardModule, // <mat-card>
  MatTableModule, // <mat-table>
  MatPaginatorModule, // <mat-paginator>
  MatSelectModule, // <mat-select>
  MatInputModule, // matInput
  MatFormFieldModule, // <mat-form-field>
  MatSnackBarModule, // MatSnackBar (inject in constructor)
  MatProgressSpinnerModule, // <mat-spinner>
  MatTreeModule, // <mat-tree>
  MatCheckboxModule, // <mat-checkbox>
  MatSliderModule, // <mat-slider>
];
```

Inject `MatSnackBar` for notifications; never use `alert()` or `console.log` for UI feedback.

### `mat-table` data source — immutable updates required

`mat-table` tracks the `dataSource` input by **reference**. When a plain array is passed, the table only re-renders when a new array reference is assigned. In-place mutations (`push`, `splice`, index-swap) leave the reference unchanged, so the table silently ignores them and the user sees no new rows.

**Wrong — table will not re-render:**

```typescript
addRow(): void {
  this.rows.push({ ...newRow });          // same reference → no re-render
}
removeRow(i: number): void {
  this.rows.splice(i, 1);                 // same reference → no re-render
}
moveUp(i: number): void {
  [this.rows[i - 1], this.rows[i]] = [this.rows[i], this.rows[i - 1]]; // same reference
}
```

**Correct — always produce a new array reference:**

```typescript
addRow(): void {
  this.rows = [...this.rows, { ...newRow }];
}
removeRow(i: number): void {
  this.rows = this.rows.filter((_, idx) => idx !== i);
}
moveUp(i: number): void {
  if (i > 0) {
    const updated = [...this.rows];
    [updated[i - 1], updated[i]] = [updated[i], updated[i - 1]];
    this.rows = updated;
  }
}
```

This rule applies to every array bound to `[dataSource]`. Existing object references inside the spread array are preserved, so `[(ngModel)]` bindings on row inputs continue to work correctly.

### Outline `mat-form-field` labels — a container must not clip or truncate them

An `appearance="outline"` field's floating `mat-label` sits ~8px *above* the field's own border box, and in its resting state the label occupies the full field width. Two ways that goes wrong:

- **Top clipping.** `mat-dialog-content` (and any `overflow: auto`/`hidden` container) clips at its padding box. A `mat-form-field` placed as the first element inside a scrolling `mat-dialog-content` has the top of its floated label shaved off. Give the first field (or its row) a `margin-top`, or add extra `padding-top` to the scroll container, so the label clears the clip edge.
- **Label truncation from a fixed width.** A field hard-sized below what its label needs (`width: 7rem` / `flex: 0 0 7rem` under a longer `mat-label`) renders the label truncated with an ellipsis. Don't fixed-width a labelled field — use `min-width` plus a growable `flex`, size it to content, or shorten the label copy.

When you add or touch a narrow numeric/select field inside a dialog, check its label isn't clipped or truncated at the mobile viewport described in §20, and check any *existing* fixed-width field in the same view while you're there.

### Don't surface validation errors before the user attempts a submit

Angular Material's default `ErrorStateMatcher` puts a `mat-form-field` into its error state — red outline, red `mat-label`, and any `<mat-error>` rendered — as soon as its control is `invalid && touched`, and Material marks a control `touched` **on blur**. For a **persistent add-style field** (a catalog dialog's "new item" row, an always-present inline input) or any field the user hasn't tried to submit yet, that means a bare focus-then-blur of a still-empty required field flashes a "required" error the user never provoked — and `MatDialog`'s default `autoFocus: 'first-tabbable'` focuses the first such field on open, so *any* later click (even one that isn't a submit) trips it.

Gate the error on an explicit **attempt flag** instead of `touched`:

```typescript
addAttempted = false;

// A per-field matcher so the field's own outline/label also stays out of
// the error state — the <mat-error> @if alone doesn't control that.
addErrorMatcher: ErrorStateMatcher = {
  isErrorState: (control) => !!control && control.invalid && this.addAttempted,
};

addItem(): void {
  if (this.addControl.invalid) {
    this.addAttempted = true;
    this.addControl.markAsTouched();
    return;
  }
  // ...on success:
  this.addAttempted = false;
}
```

- Template: `@if (addControl.invalid && addAttempted) { <mat-error>…</mat-error> }` — never `&& addControl.touched`.
- Bind the matcher on the field: `<input matInput [formControl]="addControl" [errorStateMatcher]="addErrorMatcher" />` (`ErrorStateMatcher` imports from `@angular/material/core`). Keep it a small object/class in the component — don't `provide` it globally.
- Reset the attempt flag to `false` after a successful submit so the next entry starts clean.

Whenever you add a dialog or form with a persistent add-row or an optional validated field, verify by focus-then-blur that it doesn't error before a submit is attempted.

---

## 13. CDK Tree (Folder Navigation)

Use `FlatTreeControl` with a custom data source for hierarchical folder trees:

```typescript
treeControl = new FlatTreeControl<FlatNode>(
  node => node.level,
  node => node.expandable,
);

hasChild = (_: number, node: FlatNode) => node.expandable;

private buildTree(folders: Folder[]): FlatNode[] {
  return folders.map(folder => ({
    expandable: folder.hasChildren,
    name: folder.name,
    level: folder.depth,
    path: folder.path,
  }));
}
```

---

## 14. SCSS & Styling

Define utility classes in `styles.scss` and use them across templates:

```scss
// styles.scss — global utilities
.full-height {
  height: 100%;
}
.flex-row {
  display: flex;
  flex-direction: row;
}
.flex-col {
  display: flex;
  flex-direction: column;
}
.gap-8 {
  gap: 8px;
}
.gap-16 {
  gap: 16px;
}
.p-16 {
  padding: 16px;
}
.flex-1 {
  flex: 1;
}
```

Component SCSS uses BEM-style class names:

```scss
// gallery.component.scss
.gallery-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.thumbnail-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px;
}

.thumbnail-grid .selected {
  outline: 2px solid mat.get-color($primary, 500);
}
```

---

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

## 17. Code Style Rules

- **No `any`** — use typed interfaces or `unknown` with type guards.
- **No `console.log`** — use `MatSnackBar` for user feedback; silence diagnostic output before committing.
- **No NgModules** — standalone components only.
- **No `*ngIf` / `*ngFor`** — use `@if` / `@for` (Angular 17+ control flow).
- **No global state library** — component-local properties + services are sufficient.
- **Constructor injection only** — never use `inject()` unless the project already uses it.
- **`readonly` for injected services and constants** — prevents accidental reassignment.
- **Unsubscribe on destroy** — close `EventSource` and unsubscribe from observables in `ngOnDestroy`.
- **`track` in `@for`** — always provide a track expression: `@for (item of items; track item.id)`.
- **Prefer `Set<T>`** for selection or deduplication over arrays with `indexOf`.
- **No in-place mutation of `mat-table` data sources** — always reassign the array (`this.rows = [...this.rows, ...]`) instead of mutating it with `push`, `splice`, or index assignments; the table only re-renders when the reference changes (see section 12).
- **No comments that restate the code** — add a comment only when the _why_ is non-obvious.
- **One component per file** — no barrel re-exports unless the project already uses them.

---

## 18. Multi-Step Feature Pattern

For wizard-like flows (configure → running → results):

```typescript
type StepState = "configure" | "running" | "results";

export class SyncComponent implements OnDestroy {
  stepState: StepState = "configure";
  syncResult?: SyncResult;
  private syncEventSource?: EventSource;

  startSync(): void {
    this.stepState = "running";
    this.syncEventSource = this.syncService.runSync();
    this.syncEventSource.addEventListener("sync", (event: MessageEvent) => {
      const result: SyncResult = JSON.parse(event.data);
      this.syncResult = result;
      this.stepState = "results";
      this.syncEventSource?.close();
    });
  }

  restart(): void {
    this.stepState = "configure";
    this.syncResult = undefined;
  }

  ngOnDestroy(): void {
    this.syncEventSource?.close();
  }
}
```

---

## 19. HttpOnly Cookie Authentication

When the backend uses HttpOnly cookie JWT (the correct pattern for browser apps), the Angular side stores only session metadata in `localStorage` — never the token itself. The cookie is invisible to JavaScript and sent automatically by the browser.

**AuthService — store metadata only:**

```typescript
const SESSION_KEY = 'photomanager_session';

interface LoginResponse { username: string; expiresAt: string; }
interface Session { username: string; expiresAt: number; }

login(username: string, password: string): Observable<void> {
  return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
    tap(resp => {
      const session: Session = { username: resp.username, expiresAt: new Date(resp.expiresAt).getTime() };
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    }),
    map(() => undefined)
  );
}

logout(): void {
  this.http.post('/api/auth/logout', {}).subscribe();   // clears the cookie server-side
  this.clearSession();
}

clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
}

isLoggedIn(): boolean {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return false;
  const session: Session = JSON.parse(raw);
  return session.expiresAt > Date.now();
}
```

**Auth interceptor — handle 401 only (no header injection):**

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401
          && !req.url.includes('/api/auth/login')) {
        inject(AuthService).clearSession();
        inject(Router).navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
```

There is no `Authorization` header to add. The cookie is attached by the browser for all same-origin requests (HttpClient, `<img>`, `EventSource`) without any extra code.

**Dev proxy — no changes needed:** the Angular dev proxy (`proxy.conf.json`) forwards all cookies with `/api` requests to `localhost:8080` automatically, because all requests are same-origin from the browser's perspective.

---

## 20. Mobile Viewport Verification

Any change that touches a component's template or stylesheet (new UI, a
layout tweak, a row/card that gains or loses content) must be visually
checked at a phone-sized viewport before it's considered done. A
comfortably-wide handset preset (390–430px) is not enough on its own —
real layout bugs (overlapping text, a squeezed row colliding with a
sibling element) can hide at that width and only show up on a device
meaningfully narrower than it.

**Standard check device — Samsung Galaxy S23 Ultra:**

| | value |
| --- | --- |
| CSS viewport | 384 × 824 |
| Device pixel ratio | 3.75x |
| Physical resolution | 1440 × 3088 (~500ppi) |

Verify at this size (in addition to, not instead of, any other viewport
the change specifically calls for):

- **Chrome DevTools:** open the device toolbar (Ctrl/Cmd+Shift+M), and
  either pick "Galaxy S23 Ultra" from the device list if your Chrome
  version has it, or add a custom device with the dimensions above.
- **Cypress** (component tests or a scratch/E2E spec): `cy.viewport(384,
  824)` before asserting on layout.

What to actually look for at this width, not just that the page renders:

- A flex/grid row that packs several pieces (an icon, a name, secondary
  text, a count/badge, one or more action buttons) with no `flex-wrap` —
  check whether the squeezed text column wraps cleanly onto its own
  line(s) rather than colliding with a sibling element. `min-width: 0` on
  a flex child lets its text wrap internally, but does nothing to stop it
  visually overlapping a fixed-width sibling if the row itself doesn't
  wrap or restructure.
- A non-wrapping action-button group forcing horizontal scroll.
- A wrapping action-button group whose wrapped buttons have no shared
  width rule — each one sizes to its own icon+label content, so a short
  label ends up visibly narrower than a long one once each occupies its
  own line, producing a ragged column. Give wrapped buttons a uniform
  width — stacking each one full-width is the simplest correct fix.
- Any container given a fixed pixel width rather than a relative one.

This check is part of finishing the change, the same way running the
component test for new code is — not a separate, optional pass.

---

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new routes added to `app.routes.ts`
3. Any new Material modules imported
4. How to run the tests for the new code:
   ```bash
   npx cypress run --component --spec 'src/app/<path>/your-new.component.cy.ts'
   ```
5. Any environment or proxy configuration the user should set
6. For any UI/template/CSS change, confirmation that §20's mobile viewport
   check (Samsung S23 Ultra, 384×824) was done and what — if anything — it
   caught
