# Angular Developer — Standalone Components & Routing

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

