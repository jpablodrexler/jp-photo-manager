# Angular Developer — Code Style Rules & Multi-Step Feature Pattern

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

