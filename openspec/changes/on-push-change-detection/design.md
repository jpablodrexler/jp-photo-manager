## Context

Angular's default change detection (`CheckAlways`) runs a dirty-check pass on every component in the tree after every browser event (click, scroll, keypress, timer tick). The `ThumbnailComponent` is instantiated for every visible image in the gallery (up to 50–100 at a time with virtual scrolling). Each scroll event triggers change detection on all of them. With `ChangeDetectionStrategy.OnPush`, a component is only checked when:
1. An `@Input()` reference changes (reference equality, not value equality)
2. An event originates from within the component or its children
3. `ChangeDetectorRef.markForCheck()` is explicitly called
4. An `async` pipe in the template resolves a new value

This optimization is most impactful for `ThumbnailComponent` (many instances) and `GalleryComponent` (the most logic).

## Goals / Non-Goals

**Goals:**
- Apply `OnPush` to all components in a single, consistent pass
- Ensure all mutable state patterns that bypass OnPush are replaced with immutable equivalents
- Inject `ChangeDetectorRef` where non-Angular-zone callbacks (SSE, manual subscriptions) update component state

**Non-Goals:**
- Introducing signals or NgRx (state management refactor)
- Converting observable subscriptions to `async` pipe (a separate refactoring)
- Performance benchmarking or load testing

## Decisions

### 1. Apply OnPush to all components at once

**Decision:** Update all components in a single pass rather than incrementally.

**Rationale:** Mixed-mode trees (some OnPush, some CheckAlways) are harder to reason about. A component that is still CheckAlways can mask change detection bugs in adjacent OnPush components. Applying the change uniformly eliminates the mixed state.

**Alternative considered:** Start with `ThumbnailComponent` only. Rejected because it introduces a transition period with inconsistent behavior across the component tree.

### 2. Immutable state assignments instead of mutable mutations

**Decision:** Replace all `this.someArray.push(x)`, `this.someSet.add(x)`, and index assignments with new reference creation: `this.someArray = [...data.items]`, `this.someSet = new Set([...this.someSet, x])`.

**Rationale:** OnPush uses reference equality (`===`) to detect `@Input()` changes. Mutating an array or Set in-place does not change the reference, so OnPush skips the check.

**Alternative considered:** Wrapping mutations in `markForCheck()`. Rejected because it adds noise; immutable assignments are more idiomatic and prevent future bugs.

### 3. `ChangeDetectorRef.markForCheck()` for SSE and async callbacks

**Decision:** Inject `ChangeDetectorRef` in components that receive `EventSource` events or subscribe to observables manually (not via `async` pipe). Call `cdr.markForCheck()` inside the callback.

**Rationale:** `EventSource` callbacks run outside Angular's zone by default. Without `markForCheck()`, state updates in SSE callbacks are not visible to the OnPush change detector.

**Alternative considered:** `NgZone.run()` wrapper. Both work; `markForCheck()` is lighter-weight and more explicit about the intent.

### 4. No new tests required

**Decision:** Do not add new test cases for this change.

**Rationale:** Existing tests use `fixture.detectChanges()` which runs change detection unconditionally, regardless of strategy. All existing tests should pass without modification. A test failure after this change reveals a real bug in immutable-assignment coverage.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Missed mutable mutation causes UI to stop updating | Medium | Run all existing tests; do a manual gallery walkthrough after implementation |
| SSE callbacks without `markForCheck()` cause progress bars not to update | Medium | Search all `EventSource.addEventListener` calls and ensure `cdr.markForCheck()` is present |
| `@Output()` EventEmitter events still propagate correctly | Low | `@Output()` events always trigger change detection on the parent; no change needed |
