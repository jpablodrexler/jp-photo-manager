// Real-backend E2E: teardown. Runs last (see the "99" prefix) regardless of
// which earlier release specs passed or failed, and hard-deletes every
// remaining test asset under /e2e-catalog plus purges the recycle bin — so
// this suite never leaves synthetic data permanently visible on the live
// deployment's real dashboard/analytics.
//
// /e2e-catalog is an emptyDir (see k8s/backend.yaml), so in principle a pod
// restart would also wipe it — but that's a much heavier, disruptive
// operation for routine cleanup than just deleting through the app's own
// real "Delete files" action, which this spec does instead.

import {
  realLogin,
  visitGalleryFolder,
  E2E_CATALOG_ROOT,
  TRIP_FOLDER,
  EVENTS_FOLDER,
  SYNCED_FOLDER,
  CONVERTED_FOLDER,
} from '../../support/e2e-release-helpers';

function hardDeleteEverythingInFolder(folderPath: string): void {
  visitGalleryFolder(folderPath);
  // GalleryComponent shows a mat-progress-bar while its own async GET /api/assets fetch is in
  // flight (isLoading) and only renders .asset-list-row / the "No images found" empty state once
  // it resolves. Snapshotting the body immediately after visitGalleryFolder's .current-folder
  // check (which only confirms the route loaded, not that the asset list fetch has) can catch
  // this folder mid-fetch and misread "not rendered yet" as "genuinely empty", silently skipping
  // real deletion - the same masking-bug shape teardown_runsFreshCatalogFirst's own comment
  // documents for a different root cause (stale cache instead of a load race). Wait for loading to
  // finish before treating an empty snapshot as authoritative.
  cy.get('mat-progress-bar', { timeout: 10000 }).should('not.exist');
  cy.get('body').then(($body) => {
    const rows = $body.find('.asset-list-row');
    if (rows.length === 0) {
      // Legitimate now that we've waited for the fetch to actually resolve, and because
      // teardown_runsFreshCatalogFirst (above) just forced every folder's "assets" search-cache
      // entry to be re-evicted, so this reflects real DB state, not a stale cached page.
      cy.log(`hardDeleteEverythingInFolder: ${folderPath} already empty, nothing to delete`);
      return;
    }

    cy.get('.asset-list-row').each(($row) => {
      cy.wrap($row).click();
    });
    cy.get('button[title="Actions"]').click();
    cy.contains('button', 'Delete files').click();
    cy.get('.asset-list-row').should('have.length', 0);
  });
}

describe('Release suite teardown (real backend)', () => {
  // beforeEach, not before: Cypress's default test isolation clears cookies (including the JWT)
  // between every it() block within a spec file, not just between spec files - a before()-once
  // login only survives the first test (see 01-seed-and-catalog.cy.ts's identical comment). This
  // file used before() until now, which meant every test after the first silently ran
  // unauthenticated: some failed outright (teardown_hardDeletesAllRemainingTestAssets's
  // .current-folder check correctly caught the redirect-to-login), while others with lenient
  // "if empty, skip" logic (teardown_purgesRecycleBinOfAnyStragglers) falsely reported "passing"
  // because an unauthenticated /recycle-bin redirect also has zero .asset-cell elements - the
  // exact same masking-bug shape as the stale-cache incident teardown_runsFreshCatalogFirst's own
  // comment documents, just from a different root cause.
  beforeEach(() => {
    realLogin();
  });

  it('teardown_runsFreshCatalogFirst_soFolderViewsAreNotServedFromStaleCache', () => {
    // Known incident: MoveAssetsUseCaseImpl and DeleteAssetsUseCaseImpl used to never evict the
    // `assets` Redis search-result cache (only Catalog/Upload did, via the asset.cataloged
    // Kafka event — see AssetSearchCacheInvalidationListener). That let hardDeleteEverythingInFolder
    // below silently no-op on a folder whose cached view under-reported its real contents
    // (`if (rows.length === 0) return`, with nothing to catch the lie) — confirmed the hard way:
    // this teardown reported "passing" across many real runs while 27 stale/duplicate asset rows
    // from earlier runs sat untouched in Postgres the whole time. That gap is now fixed at the
    // source (both use cases evict the affected folder(s) synchronously), but this catalog run
    // is kept as defense in depth: it forces every /e2e-catalog folder's cache entry to be
    // freshly re-evicted via the same asset.cataloged/asset.deleted events Catalog has always
    // used, AND its own afterStep cleanup (CatalogAssetItemWriter) hard-deletes any DB row whose
    // backing file the pod's emptyDir no longer has on disk — self-healing any orphaned rows a
    // *previous* run's teardown left behind, not just this run's own data.
    visitGalleryFolder(E2E_CATALOG_ROOT);
    cy.get('button[title="Run catalog"]').click();
    cy.get('.catalog-icon.spinning', { timeout: 10000 }).should('exist');
    cy.get('.catalog-icon.spinning', { timeout: 120000 }).should('not.exist');
    cy.get('.catalog-status-text').should('contain.text', 'Idle');
  });

  it('teardown_clearsSyncAndConvertConfigurations', () => {
    // Sync/Convert directory-pair configurations are persisted server-side, independent of any
    // asset or folder, so hardDeleteEverythingInFolder below never touches them - left uncleared,
    // a run's own Add'd row silently accumulates and breaks the next run's
    // 06-sync.cy.ts/07-convert.cy.ts, both of which assume the config table starts empty (confirmed:
    // a leftover row from an earlier run made cy.get('tbody tr').should('have.length', 1) see 2
    // rows after that spec's own "Add" click).
    cy.request('PUT', '/api/sync/configuration', []);
    cy.request('PUT', '/api/convert/configuration', []);
  });

  it('teardown_hardDeletesAllRemainingTestAssets', () => {
    // E2E_CATALOG_ROOT itself must be included, not just the trip/events/synced/converted
    // subfolders: 01-seed-and-catalog.cy.ts uploads flat into the root before moving assets
    // into subfolders, so any run that was ever interrupted between upload and move (a real
    // incident during this suite's own development) leaves orphaned copies sitting directly
    // in the root that this teardown would otherwise never visit or delete, silently
    // accumulating across runs (confirmed the hard way: stale root-level duplicates from
    // earlier partial runs caused widespread false-positive "too many assets found"
    // failures in a later full run before this fix).
    [E2E_CATALOG_ROOT, TRIP_FOLDER, EVENTS_FOLDER, SYNCED_FOLDER, CONVERTED_FOLDER].forEach(
      hardDeleteEverythingInFolder,
    );
  });

  it('teardown_purgesRecycleBinOfAnyStragglers', () => {
    cy.visit('/recycle-bin');
    cy.get('body').then(($body) => {
      if ($body.find('.asset-cell').length === 0) return;
      cy.get('button[title="Empty Recycle Bin"]').click();
      cy.contains('Recycle bin is empty.').should('exist');
    });
  });
});
