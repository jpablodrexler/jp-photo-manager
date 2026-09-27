# Angular Developer — Templates & Component State (RxJS)

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

