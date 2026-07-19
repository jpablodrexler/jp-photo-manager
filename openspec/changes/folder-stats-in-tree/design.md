## Context

The `FolderNavComponent` uses Angular CDK `FlatTreeControl` to render a folder hierarchy fetched from `GET /api/folders`. Each node is a `FlatNode` with `name`, `path`, `level`, and `expandable`. Adding `assetCount` and `totalSizeBytes` to each node requires either embedding stats in the existing folder list response or fetching them separately on demand.

## Goals / Non-Goals

**Goals:**
- `GET /api/folders/stats?path=<folderPath>` returns `{ assetCount: long, totalSizeBytes: long }` for non-deleted assets in that folder (non-recursive)
- `FolderNavComponent` fetches stats for each node when the tree expands and caches the result for the session
- Stats are shown inline: `Photos (42 · 1.2 GB)`

**Non-Goals:**
- Recursive folder stats (subtree totals) — only direct children are counted
- Real-time stats updates as assets are added/deleted during the same session
- Stats in the folder list API response (kept separate to avoid breaking existing callers)

## Decisions

### 1. Separate stats endpoint

**Decision:** A dedicated `GET /api/folders/stats?path=...` endpoint rather than embedding stats in the existing folder list response.

**Rationale:** The folder list is paginated and loaded before the user expands nodes. Embedding stats would require computing them for all folders upfront. A separate endpoint allows lazy loading per node.

### 2. On-expand lazy loading with in-memory cache

**Decision:** `FolderNavComponent` calls the stats endpoint when a node is first expanded. Results are stored in a `Map<string, FolderStats>` for the component lifetime to avoid repeated requests.

**Rationale:** Users typically expand a small subset of folders; prefetching all stats is wasteful. The map prevents duplicate requests on collapse/re-expand.

### 3. Non-recursive count

**Decision:** Count only direct-child assets (`folder_path = :path`), not the entire subtree.

**Rationale:** Recursive counts require a CTE or recursive query and are significantly more expensive. Direct counts are O(1) with an index on `folder_path`.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Stats become stale after catalog runs | Low | Stats are informational; reload clears the in-memory cache |
| Many expanded nodes generate many requests | Low | Cache prevents repeated requests for the same path |
