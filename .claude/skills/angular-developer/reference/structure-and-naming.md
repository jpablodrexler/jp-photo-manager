# Angular Developer — Directory Structure & Naming Conventions

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

