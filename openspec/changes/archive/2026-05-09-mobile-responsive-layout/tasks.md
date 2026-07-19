## 1. AppComponent — TypeScript

- [x] 1.1 In `app.component.ts`, add imports: `BreakpointObserver, Breakpoints` from `@angular/cdk/layout`; `Subscription` from `rxjs`; `OnDestroy` from `@angular/core`
- [x] 1.2 Implement `OnDestroy`; add private field `isMobile = false` and `private bpSub: Subscription`
- [x] 1.3 In `ngOnInit`, subscribe to `breakpointObserver.observe([Breakpoints.Handset])`; in the callback set `this.isMobile = result.matches`; store the subscription in `bpSub`
- [x] 1.4 In `ngOnDestroy`, call `this.bpSub.unsubscribe()`
- [x] 1.5 Add `MatMenuModule` and `MatIconModule` to the `imports` array of the standalone component (if not already present)

## 2. AppComponent — Template

- [x] 2.1 In `app.component.html`, inside the `@if (isLoggedIn)` block, add a hamburger button visible only on mobile:
  ```html
  @if (isMobile) {
    <button mat-icon-button [matMenuTriggerFor]="navMenu" aria-label="Open navigation">
      <mat-icon>menu</mat-icon>
    </button>
    <mat-menu #navMenu="matMenu">
      <button mat-menu-item routerLink="/home"><mat-icon>home</mat-icon> Home</button>
      <button mat-menu-item routerLink="/gallery"><mat-icon>photo</mat-icon> Gallery</button>
      <button mat-menu-item routerLink="/sync"><mat-icon>sync</mat-icon> Sync</button>
      <button mat-menu-item routerLink="/convert"><mat-icon>transform</mat-icon> Convert</button>
      <button mat-menu-item routerLink="/duplicates"><mat-icon>content_copy</mat-icon> Duplicates</button>
      <button mat-menu-item routerLink="/admin/users"><mat-icon>manage_accounts</mat-icon> Users</button>
      <button mat-menu-item (click)="logout()"><mat-icon>logout</mat-icon> Logout</button>
    </mat-menu>
  }
  ```
- [x] 2.2 Wrap the existing inline nav links and logout button in `@if (!isMobile) { ... }` so they are hidden on mobile

## 3. GalleryComponent — TypeScript

- [x] 3.1 In `gallery.component.ts`, add imports: `BreakpointObserver, Breakpoints` from `@angular/cdk/layout`; `Subscription` from `rxjs`; `OnDestroy` from `@angular/core`
- [x] 3.2 Implement `OnDestroy`; add fields `isMobile = false`, `sidenavOpen = true`, `private bpSub: Subscription`
- [x] 3.3 In `ngOnInit`, subscribe to `breakpointObserver.observe([Breakpoints.Handset])`; in the callback: `this.isMobile = result.matches; if (result.matches) { this.sidenavOpen = false; } else { this.sidenavOpen = true; }`
- [x] 3.4 Add method `toggleSidenav(): void { this.sidenavOpen = !this.sidenavOpen; }`
- [x] 3.5 In `onFolderSelected(path: string)`, after setting `this.currentFolder` and calling `this.loadAssets(0)`, add: `if (this.isMobile) { this.sidenavOpen = false; }`
- [x] 3.6 In `ngOnDestroy`, call `this.bpSub.unsubscribe()`
- [x] 3.7 Add `MatSidenavModule` to the `imports` array of the standalone component

## 4. GalleryComponent — Template

- [x] 4.1 Replace the root `<div class="gallery-layout full-height flex-row">` structure with:
  ```html
  <mat-sidenav-container class="gallery-layout full-height">
    <mat-sidenav
      [mode]="isMobile ? 'over' : 'side'"
      [opened]="sidenavOpen"
      (closedStart)="sidenavOpen = false"
      class="folder-sidenav">
      <app-folder-nav (folderSelected)="onFolderSelected($event)" />
    </mat-sidenav>

    <mat-sidenav-content class="flex-col">
      <!-- existing toolbar, thumbnail grid, viewer, status bar -->
    </mat-sidenav-content>
  </mat-sidenav-container>
  ```
- [x] 4.2 In the gallery toolbar, add a toggle button that is visible only on mobile, before the `<span class="current-folder">`:
  ```html
  @if (isMobile) {
    <button mat-icon-button (click)="toggleSidenav()" aria-label="Toggle folder panel">
      <mat-icon>menu</mat-icon>
    </button>
  }
  ```
- [x] 4.3 Remove the old `<div class="folder-tree">` wrapper (it is now the `<mat-sidenav>`)

## 5. GalleryComponent — SCSS

- [x] 5.1 In `gallery.component.scss`, remove any `.folder-tree` width rule that was referenced from the component (the width is now managed by the sidenav and global styles)
- [x] 5.2 Add:
  ```scss
  .folder-sidenav {
    width: 280px;
    background-color: #1a1a1a;
    border-right: 1px solid rgba(255, 255, 255, 0.12);
  }

  .gallery-layout {
    height: 100%;
  }
  ```
- [x] 5.3 Ensure `mat-sidenav-content` fills remaining space by confirming `.flex-col` is applied to `<mat-sidenav-content>` (via the class binding in the template)

## 6. Global Styles

- [x] 6.1 In `styles.scss`, add a responsive block at the end of the file:
  ```scss
  @media (max-width: 767px) {
    .thumbnail-grid {
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
      gap: 4px;
      padding: 8px;
    }

    .thumbnail-card img {
      height: 110px;
    }

    .gallery-toolbar .current-folder {
      max-width: 120px;
    }
  }
  ```

## 7. Frontend Tests

- [x] 7.1 In `app.component.cy.ts` (create if not present), mount `AppComponent` with a stub `BreakpointObserver` that emits `{ matches: true }`; assert the hamburger `button[aria-label="Open navigation"]` is visible; assert the inline nav links are NOT rendered
- [x] 7.2 Add test: stub `BreakpointObserver` emitting `{ matches: false }`; assert the hamburger button is NOT rendered; assert the inline `<a>` nav links are visible
- [x] 7.3 In `gallery.component.cy.ts`, add test: stub `BreakpointObserver` emitting `{ matches: true }`; mount `GalleryComponent` with stub services; assert `mat-sidenav[mode="over"]` exists; assert the folder toggle button `button[aria-label="Toggle folder panel"]` is visible
- [x] 7.4 Add test: stub `BreakpointObserver` emitting `{ matches: false }`; assert `mat-sidenav[mode="side"]` exists; assert the folder toggle button is NOT rendered
- [x] 7.5 Add test: on mobile, simulate folder selection; assert `sidenavOpen` becomes `false` (verify the sidenav `[opened]` binding is falsy after the event)
- [x] 7.6 Run `npm test` and confirm all tests pass
