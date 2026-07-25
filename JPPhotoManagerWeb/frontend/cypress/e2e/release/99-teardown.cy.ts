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
  cy.get('body').then(($body) => {
    const rows = $body.find('.asset-list-row');
    if (rows.length === 0) {
      // Legitimate only because teardown_runsFreshCatalogFirst (above) just forced every
      // folder's "assets" search-cache entry to be re-evicted, so this reflects real DB state,
      // not a stale cached page — see that test's comment for the incident this guards against.
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
  before(() => {
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
