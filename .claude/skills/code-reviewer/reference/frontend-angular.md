# Code Reviewer — Frontend: Angular Conventions

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 10. Frontend: Angular Conventions

### 10.1 Component structure

🔴 Flag any `NgModule` — all components must be `standalone: true`.

🔴 Flag `*ngIf` or `*ngFor` in templates — use `@if` / `@for` (Angular 17+
control flow) instead.

🟡 Flag `@for` without a `track` expression.

🟡 Flag a component's `imports: []` that includes a module the template
doesn't actually use.

🟡 Flag a component that subscribes to observables in the constructor instead
of `ngOnInit`.

🔴 Flag a component that opens an `EventSource` but doesn't close it in
`ngOnDestroy`.

### 10.2 Services

🔴 Flag a service that subscribes to an `Observable` internally — services
must return `Observable<T>` and let the component subscribe.

🟡 Flag a service without `providedIn: 'root'`.

🟡 Flag HTTP logic inside a component — API calls belong in `core/services/`.

🟡 Flag a service file that contributes no logic of its own and just
re-exports or aliases another service (e.g.
`export { FooService as BarService } from './foo.service';`, or a class whose
every method is a one-line passthrough to an injected service of a different
name for the same capability). Same test as the backend port/adapter case in
§1.2: the question is whether it adds logic, not whether something imports
it. Delete it and repoint any importers to the real service — see the
`AudioPlayerService` entry in §15.

### 10.3 Routing

🟡 Flag a new feature route that is not lazy-loaded with `loadComponent`.

🟡 Flag a direct import of a feature component in `app.routes.ts` instead of
a dynamic import.

### 10.4 Material Component Layout Gotchas

🟡 Flag a `mat-icon` placed inside `mat-card-avatar` with no matching CSS
rule sizing and centering it (`width`/`height`, `font-size`/`line-height`,
flex-centered). `mat-card-avatar`'s built-in sizing (`object-fit: cover`,
`overflow: hidden`) is designed for an `<img>` — left unstyled for a
`mat-icon`, the glyph renders oversized and gets clipped by the avatar
circle down to an unrecognizable fragment.

🟢 Flag a `mat-form-field` placed as the very first element in
`mat-card-content`, directly under a `mat-card-title` with nothing else
between them, that has no `margin-top` of its own. The field's
floating-label notch plus `mat-card-header`'s tight bottom padding tends to
read as the field crowding the title above it.

🟡 Flag the app shell's root `mat-toolbar` if it stays pinned on screen by
neither of this app's two valid mechanisms: an explicit `position: fixed`/
`sticky` rule with a matching sibling offset (e.g. a `margin-top` equal to
the toolbar's height, so taking it out of flow doesn't overlap the content
below it), *or* — the mechanism `app.component.scss` actually uses — being
a normal-flow flex-column sibling of a `flex: 1; min-height: 0;`,
independently-`overflow-y: auto` content container, so only that container
scrolls and the toolbar (never taken out of flow at all) never moves.
`<mat-toolbar>` has no built-in pinning of its own — a toolbar with
neither mechanism applied sits in normal document flow and scrolls out of
the viewport with page content.

🟢 Flag a **new** app-shell, dialog, or full-page layout that scrolls the
whole document under a `position: fixed`/`sticky` header instead of
confining scroll to a `flex: 1; min-height: 0; overflow-y: auto` content
container the way `app.component.scss` already does — a document-level
scrollbar then spans the full viewport height and runs past the header's
own boundary. Prefer a `height: 100vh`/`100dvh` flex column with the
header as a normal-flow sibling above the scrolling content region; only
reach for `position: sticky`/`fixed` plus a compensating offset if
document-level scroll is genuinely intended elsewhere on the same page.
This applies to newly-introduced scroll regions and shell restructures —
the app's existing shell already follows the correct pattern, so don't
re-flag it on an unrelated change.

🟡 Flag a row-like flex container (a list row, a card's action bar, any
element packing an icon/name/secondary-text/count/buttons on one line)
whose CSS gives it `display: flex` with no `flex-wrap` and no
narrow-viewport fallback (a media query, a container query, or a
restructure into a two-line layout below a breakpoint). At full width
this looks fine; at a real phone width the flex children fight over too
little space, and a shrinking text child can visually overlap a
fixed-width sibling instead of the row wrapping cleanly. Confirm the
change was actually checked at `angular-developer` §20's standard mobile
check device, not just eyeballed at desktop width or a wider handset
preset.

🟢 Flag a `flex-wrap: wrap` action-button row (a toolbar of several
buttons) with no shared width rule on its buttons. Wrapping alone stops
the horizontal-scroll problem, but each button still sizes to its own
icon+label content by default — once a narrow viewport forces one button
per line, a short label ends up visibly narrower than a long one, reading
as a ragged, unpolished column. The buttons should share a uniform width
once wrapped — stacking each full-width is the simplest correct fix.

🟡 Flag an outline `mat-form-field` whose floating `mat-label` can be
**clipped or truncated**:
- Placed as the first element inside a scrollable container (`mat-dialog-
  content`, or any `overflow: auto`/`hidden` region) with no `margin-top`
  on the field or its row — the label sits a few pixels above the field's
  border box, and the container clips it at its padding box once the
  content scrolls. This is the scroll-clipping counterpart of the
  `mat-card-content` crowding flag above.
- Hard-sized narrower than its label needs (`width`/`flex: 0 0 <fixed>`
  under a longer `mat-label`) — the label renders with an ellipsis. A
  labelled field should use `min-width` plus a growable `flex`, size to
  content, or carry a shorter label instead.

🟡 Flag a `mat-form-field` that is meant to fill its row or card but has
no width rule of its own (`width: 100%`, or a growable `flex: 1` with
`min-width: 0`). Material form fields size to their default intrinsic
width, so a field inside a flex row or a wrapper that also holds a hint can
render far narrower than its container; a width cap placed on a wrapper
that holds both the field and its hint caps the hint too. Check the
reused-form-class case as well — a width class written for one form that
silently applies to a second.

🟡 Flag a `mat-form-field` bound to a `FormControl` with validators —
typically a catalog/dialog **add row** or an otherwise-optional field —
that surfaces its error state on a bare focus-then-blur, before the user
has attempted the submit/add. Angular Material's default
`ErrorStateMatcher` fires on `invalid && touched`, Material marks a
control `touched` on blur, and `MatDialog` auto-focuses the first field on
open — so any later click trips a "required" error the user never
provoked. Look for a template `<mat-error>` gated on `control.touched`
(rather than an explicit "attempted" signal), and for a validated add-row
field with **no** custom `[errorStateMatcher]` (the `<mat-error>` `@if`
alone doesn't keep the field's own red outline/label out of the error
state). The fix pattern: an `xAttempted` signal set only on an invalid
submit, reset after success, gating both the `@if` and a per-field
matcher.

---
