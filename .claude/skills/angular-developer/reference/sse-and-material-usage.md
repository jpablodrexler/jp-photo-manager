# Angular Developer — Server-Sent Events & Angular Material Usage

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

