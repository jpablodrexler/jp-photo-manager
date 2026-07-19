## Context

`FindDuplicatedAssetsUseCase` returns all non-deleted assets grouped by hash — each group has two or more assets with identical content. The duplicates page (`DuplicatesComponent`) shows these groups and allows manual selection of which to delete. Manual cleanup is impractical at scale. The existing `SoftDeleteAssetUseCase` handles individual asset deletion; `AutoResolveDuplicatesUseCaseImpl` will iterate all groups, apply the policy to pick the "winner", and delegate soft-deletion of the others.

## Goals / Non-Goals

**Goals:**
- Implement four resolution policies as a backend `ResolutionPolicy` enum
- Soft-delete all non-winners via the existing `SoftDeleteAssetUseCase`
- Provide a confirmation dialog showing how many assets will be deleted before executing
- Support `KEEP_PREFERRED_FOLDER` with an optional `folderPath` parameter

**Non-Goals:**
- Hard deletion (permanent purge) — soft-delete is used throughout the app
- Scoped resolution (resolving only selected groups) — this feature resolves all groups at once
- Undo functionality — the recycle bin covers restoration

## Decisions

### 1. Policy logic in the use case, not in a DB query

**Decision:** Load all duplicate groups into memory in `AutoResolveDuplicatesUseCaseImpl`, apply the policy in Java, and collect the IDs to soft-delete.

**Rationale:** The `KEEP_HIGHEST_RESOLUTION` policy compares `pixel_width * pixel_height`; the `KEEP_PREFERRED_FOLDER` policy matches `folderPath` using Java `String.startsWith()`. These are simpler to express and test in Java than as JPQL subqueries. Catalog sizes are bounded, so loading duplicate groups into memory is safe.

**Alternative considered:** SQL `ROW_NUMBER() OVER (PARTITION BY hash ORDER BY ...)` to identify winners in the database. Rejected because `KEEP_PREFERRED_FOLDER` does not translate cleanly to SQL, and testing policy logic in Java is simpler.

### 2. Delegate to existing `SoftDeleteAssetUseCase`

**Decision:** Call `SoftDeleteAssetUseCase.execute(assetId)` for each non-winner rather than issuing a bulk `UPDATE` directly.

**Rationale:** `SoftDeleteAssetUseCase` is the authoritative soft-delete path; it handles all side effects (cache eviction, audit fields). Bypassing it risks inconsistency.

**Alternative considered:** Bulk `UPDATE assets SET deleted_at = NOW() WHERE id IN (...)`. Rejected because it skips `SoftDeleteAssetUseCase` side effects.

### 3. Dry-run mode for confirmation preview

**Decision:** The frontend calls `POST /api/assets/duplicates/auto-resolve` with `dryRun: true` first to get the count of assets that would be deleted, shows it, then on confirm calls with `dryRun: false`.

**Rationale:** Destructive operations should show a confirmation step with meaningful information. A server-side dry run uses the authoritative current state.

**Alternative considered:** Client-side preview based on already-loaded duplicate groups. Rejected because the loaded groups may be stale.

### 4. `KEEP_PREFERRED_FOLDER` fallback

**Decision:** If none of the assets in a group match the preferred folder path, fall back to `KEEP_OLDEST`.

**Rationale:** The user's preferred folder may not contain a copy for every group. A silent fallback to a deterministic policy is better than skipping a group.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| All duplicates in a group are soft-deleted if policy is ambiguous | Low | Policy implementations always retain exactly one asset per group; add unit tests for each policy |
| Long-running for large catalogs | Low | Soft-delete is a simple `UPDATE` per asset; typical catalogs complete in seconds |
| Dry run count can differ from actual deletion count due to concurrent changes | Low | Show the dry-run count as an estimate; note this in the UI |
