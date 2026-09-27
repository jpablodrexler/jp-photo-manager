# Angular Developer — Services & Models

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

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

