// Real-backend E2E: Gallery — folder navigation, thumbnails, viewer, rating,
// tagging, and filename search. Depends on 01-seed-and-catalog.cy.ts.

import { realLogin, visitGalleryFolder, TRIP_FOLDER } from '../../support/e2e-release-helpers';

describe('Gallery (real backend)', () => {
  beforeEach(() => {
    realLogin();
  });

  it('folderNav_afterCatalog_listsE2eCatalogTripAndEventsSubfolders', () => {
    cy.visit('/gallery');
    cy.contains('mat-tree-node', 'trip', { timeout: 15000 }).should('exist');
    cy.contains('mat-tree-node', 'events').should('exist');
  });

  it('galleryFolder_tripSelected_showsThreeRealThumbnails', () => {
    visitGalleryFolder(TRIP_FOLDER);
    cy.get('.asset-list-row').should('have.length', 3);
    cy.get('.asset-list-row img.list-thumb').each(($img) => {
      cy.wrap($img).should('have.attr', 'src').and('match', /\/api\/assets\/\d+\/thumbnail/);
    });
  });

  it('viewer_openAssetAndRate_ratingPersistsAcrossReopen', () => {
    visitGalleryFolder(TRIP_FOLDER);
    cy.get('.asset-list-row').eq(2).dblclick(); // the blue-square asset, untouched by other specs
    cy.get('.viewer-image').should('exist');

    cy.get('[data-cy="rate-star-4"]').click();
    cy.get('[data-cy="rate-star-4"] mat-icon').should('have.text', 'star');
    cy.get('[data-cy="rate-star-5"] mat-icon').should('have.text', 'star_border');

    // Close and re-open the same asset to verify the rating round-tripped
    // through the real backend (AssetRepository.save via RateAssetUseCase)
    // rather than only updating local component state.
    cy.get('button mat-icon').contains('grid_view').click();
    cy.get('.asset-list-row').eq(2).dblclick();
    cy.get('[data-cy="rate-star-4"] mat-icon').should('have.text', 'star');
  });

  it('bulkTag_appliedToSelectedAsset_persistsAndFiltersable', () => {
    const tag = `e2e-release-${Date.now()}`;
    visitGalleryFolder(TRIP_FOLDER);

    cy.get('.asset-list-row').eq(1).click(); // select (single click), not dblclick
    cy.get('button[title="Actions"]').click();
    cy.contains('button', 'Tag selected').click();

    cy.get('input[placeholder="Add tag…"]').type(`${tag}{enter}`);
    cy.contains('button', 'Apply').click();
    cy.get('mat-dialog-container').should('not.exist');

    // Filter the folder by the newly-applied tag and confirm exactly the
    // tagged asset is returned by a real backend query, not a cached list.
    cy.get('input[placeholder="Filter by tag…"]').type(`${tag}{enter}`);
    cy.get('.asset-list-row', { timeout: 10000 }).should('have.length', 1);
  });

  it('search_byFilename_filtersToMatchingAssetOnly', () => {
    visitGalleryFolder(TRIP_FOLDER);
    // force: true -- the mat-form-field label briefly overlaps the input during
    // its floating-label transition right after the page loads; that's a real
    // Angular Material animation, not an actual real-user obstruction, and
    // Cypress's default actionability check has no way to tell the difference.
    cy.get('.filter-search input[matInput]').type('green', { force: true });
    cy.get('.asset-list-row', { timeout: 10000 }).should('have.length', 1);
    cy.get('.asset-list-row .list-filename').should('contain.text', 'green');
  });
});
