## Why

The duplicates page already surfaces duplicate groups and lets users manually choose which copy to keep, but resolving dozens of groups one-by-one is tedious. A one-click "Clean up automatically" action with a configurable policy (keep oldest, newest, highest resolution, or preferred folder) lets users eliminate all duplicates in seconds without reviewing each group individually.

## What Changes

- New `ResolutionPolicy` enum (`KEEP_OLDEST`, `KEEP_NEWEST`, `KEEP_HIGHEST_RESOLUTION`, `KEEP_PREFERRED_FOLDER`) in `domain/enums/`
- New use-case interface `AutoResolveDuplicatesUseCase` in `domain/port/in/catalog/` with `execute(ResolutionPolicy, String)` returning an `AutoResolveResult` summary record
- New use-case implementation `AutoResolveDuplicatesUseCaseImpl` in `application/usecase/catalog/` that calls `GetDuplicatedAssetsUseCase` then delegates non-kept assets to `DeleteAssetsUseCase`
- New HTTP request DTO `AutoResolveDuplicatesRequest` in `infrastructure/web/dto/`
- New endpoint `POST /api/assets/duplicates/auto-resolve` in `AssetController` wired to `AutoResolveDuplicatesUseCase`
- New Angular dialog component `AutoResolveDialogComponent` in `features/duplicates/auto-resolve-dialog/` with policy selector and optional preferred-folder input
- `AssetService` extended with `autoResolveDuplicates(policy, preferredFolderPath?)` method
- `DuplicatesComponent` updated with a "Clean up automatically" button that opens the dialog and reloads groups on success

## Capabilities

### New Capabilities

- `duplicate-auto-resolve`: Automatically resolves all duplicate groups in one operation using a user-chosen policy, soft-deleting the non-kept copies via the existing delete path.

### Modified Capabilities

- `GET /api/assets/duplicates` — unchanged; still used to reload the list after auto-resolve completes.
- `DuplicatesComponent` — gains a "Clean up automatically" button and integrates the new dialog.

## Impact

- `domain/enums/ResolutionPolicy.java` — new file
- `domain/port/in/catalog/AutoResolveDuplicatesUseCase.java` — new file
- `application/usecase/catalog/AutoResolveDuplicatesUseCaseImpl.java` — new file
- `application/dto/AutoResolveResult.java` — new file (record)
- `infrastructure/web/dto/AutoResolveDuplicatesRequest.java` — new file
- `infrastructure/web/controller/AssetController.java` — new `POST /api/assets/duplicates/auto-resolve` endpoint
- `core/services/asset.service.ts` — new `autoResolveDuplicates()` method
- `features/duplicates/auto-resolve-dialog/auto-resolve-dialog.component.ts` — new dialog
- `features/duplicates/duplicates.component.ts` — "Clean up automatically" button + dialog integration
