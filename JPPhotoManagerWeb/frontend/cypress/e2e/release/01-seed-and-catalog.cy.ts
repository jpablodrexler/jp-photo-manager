// Real-backend E2E: seeds the shared test dataset every later spec in this
// suite depends on, then exercises the catalog run + SSE progress feature.
//
// Seeding is a two-step real-feature chain, not a backend/kubectl
// workaround:
//   1. Upload all 6 fixtures flat into E2E_CATALOG_ROOT (DropZoneComponent
//      — a real multipart POST + real per-file SSE processing stream).
//      Uploading directly into subfolders isn't possible: UploadAssetUseCaseImpl
//      requires the destination to already be a cataloged Folder row and
//      never creates one — confirmed while building this suite (uploading
//      into a brand-new /e2e-catalog/trip fails with a 404 "Folder not
//      found in catalog", even though the drop-zone UI itself renders for
//      any folder path with no existence check at all).
//   2. Move the trip/events subsets into their real subfolders via a real
//      POST /api/assets/move (see moveAssetsToFolder) — Move, unlike
//      Upload, does create the destination Folder row on the fly.
// This spec doubles as real E2E coverage for both the Upload and Move
// features. Must run before every other spec in cypress/e2e/release/
// (hence the "01" prefix — Cypress runs spec files in the order
// `cypress run` globs them, which for a consistent OS is lexicographic).

import {
  realLogin,
  visitGalleryFolder,
  uploadFixturesToFolder,
  moveAssetsToFolder,
  E2E_CATALOG_ROOT,
  TRIP_FOLDER,
  EVENTS_FOLDER,
} from '../../support/e2e-release-helpers';

// cy.request() is a one-shot command - chaining .should(...) off it re-checks the same already-
// fetched response on retry, it does not re-issue the request. Polling a value that only changes
// server-side (like lastCatalogCompletedAt, once the real backend catalog job finishes) needs an
// explicit retry loop that re-fetches each time instead.
function waitForLastCatalogCompletedAtToChange(before: string, attemptsLeft = 20): void {
  cy.request('/api/home/stats').then((res) => {
    if (res.body.lastCatalogCompletedAt !== before || attemptsLeft <= 0) {
      expect(res.body.lastCatalogCompletedAt).not.to.eq(before);
      return;
    }
    cy.wait(1000);
    waitForLastCatalogCompletedAtToChange(before, attemptsLeft - 1);
  });
}

describe('Seed data upload + catalog run (real backend)', () => {
  // beforeEach, not before: Cypress's default test isolation clears cookies
  // (including the JWT) between every `it()` block within a spec file, not
  // just between spec files — a `before()`-once login only survives the
  // first test. Every other spec in this suite already re-logs-in per test
  // for the same reason; this file is just the one where a multi-step
  // sequential flow (upload → move → verify → catalog run) made a
  // once-only `before()` look tempting despite that not actually working.
  beforeEach(() => {
    realLogin();
  });

  it('upload_sixImagesToE2eCatalogRoot_allReachDoneStatus', () => {
    uploadFixturesToFolder(E2E_CATALOG_ROOT, [
      'e2e-release/trip/red-square.png',
      'e2e-release/trip/green-square.png',
      'e2e-release/trip/blue-square.png',
      'e2e-release/events/yellow-square.png',
      'e2e-release/events/magenta-square.png',
      // Byte-identical to trip/red-square.png — seeds the Duplicates feature.
      'e2e-release/events/red-square-duplicate.png',
    ]);
    cy.get('.upload-item .status-done').should('have.length', 6);
  });

  it('moveAssets_intoTripAndEventsSubfolders_realMoveApiCreatesThem', () => {
    moveAssetsToFolder(
      ['red-square.png', 'green-square.png', 'blue-square.png'],
      TRIP_FOLDER,
    );
    moveAssetsToFolder(
      ['yellow-square.png', 'magenta-square.png', 'red-square-duplicate.png'],
      EVENTS_FOLDER,
    );
  });

  it('movedAssets_areVisibleInGallery_underTheirNewFolders', () => {
    visitGalleryFolder(TRIP_FOLDER);
    cy.get('.asset-list-row', { timeout: 15000 }).should('have.length', 3);

    visitGalleryFolder(EVENTS_FOLDER);
    cy.get('.asset-list-row', { timeout: 15000 }).should('have.length', 3);
  });

  it('catalogRun_triggeredFromGalleryToolbar_completesAndAdvancesLastCatalogCompletedAt', () => {
    // CatalogProgressFooterComponent's spinner only flips to the running state upon receiving at
    // least one real, server-driven 'catalog' SSE progress event (see app.component.ts) - a run
    // over an already-fully-catalogued /e2e-catalog tree with nothing new to process can complete
    // with zero such intermediate ticks, so the spinner may never visibly appear even though the
    // job genuinely executes and completes (confirmed via direct Postgres query across many runs:
    // job executions land COMPLETED in ~4-5s every time, with no rate-limiting or orphaned-execution
    // issue - see CatalogAssetsUseCaseImpl's staleness recovery). Assert completion via the durable
    // lastCatalogCompletedAt timestamp advancing instead of the transient spinner state, which
    // this suite's own history shows is not a reliable signal for a no-op scan.
    cy.request('/api/home/stats').its('body.lastCatalogCompletedAt').then((before) => {
      visitGalleryFolder(TRIP_FOLDER);

      // Admin-only catalog trigger button (manage_search icon) in the gallery toolbar.
      cy.get('button[title="Run catalog"]').click();

      // Generous window since Spring Batch job startup can be slow on a constrained cluster (see
      // k8s/backend.yaml's startupProbe comment for the class of latency this cluster can see).
      cy.get('.catalog-status-text', { timeout: 60000 }).should('contain.text', 'Idle');
      waitForLastCatalogCompletedAtToChange(before);
    });
  });
});
