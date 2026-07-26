// Real-backend E2E: Albums. Depends on 01-seed-and-catalog.cy.ts for an
// asset to add to the album.

import { realLogin, visitGalleryFolder, EVENTS_FOLDER } from '../../support/e2e-release-helpers';

describe('Albums (real backend)', () => {
  const albumName = `E2E Release Album ${Date.now()}`;

  beforeEach(() => {
    realLogin();
  });

  it('album_createManualAlbum_appearsInAlbumsGrid', () => {
    cy.visit('/albums');
    cy.get('button[title="New album"]').click();
    cy.get('.create-form input[matInput]').first().type(albumName);
    cy.contains('.create-form-actions button', 'Create').click();
    cy.contains('.album-card', albumName, { timeout: 10000 }).should('exist');
  });

  it('album_addAssetFromGallery_showsUpInAlbumDetail', () => {
    // Find the album's id by opening it from the grid rather than assuming
    // it's freshly created at a known id — real backend state, not a mock.
    cy.visit('/albums');
    cy.contains('.album-card', albumName).find('a').contains('Open').click();
    cy.url().should('match', /\/albums\/\d+$/);
    cy.location('pathname').then((path) => {
      const albumId = path.split('/').pop();

      visitGalleryFolder(EVENTS_FOLDER);
      cy.get('.asset-list-row').first().find('.list-album-btn').click();
      // AddToAlbumDialogComponent — select the album we just created and confirm.
      cy.contains('mat-radio-button', albumName).click();
      cy.contains('button', 'Add').click();
      // GalleryComponent.addToAlbum() fires the real POST /api/albums/{id}/assets only after
      // the dialog's afterClosed() resolves - navigating away immediately (as a plain cy.visit
      // right after the click used to do here) can abort that still-in-flight request before it
      // reaches the server, confirmed via a direct API reproduction: the add-then-read sequence
      // always succeeds when done back-to-back outside the UI, so the gap is this test racing
      // ahead of the app's own async request, not a backend defect. Wait for the confirmation
      // snackbar (see the "Added to ..." message in gallery.component.ts) before navigating.
      cy.contains('Added to').should('exist');

      cy.visit(`/albums/${albumId}`);
      cy.get('.asset-wrapper', { timeout: 10000 }).should('have.length.greaterThan', 0);
    });
  });

  it('album_delete_removesFromAlbumsGrid', () => {
    cy.visit('/albums');
    cy.contains('.album-card', albumName)
      .find('button[title="Delete album"]')
      .click();
    cy.contains('.album-card', albumName).should('not.exist');
  });
});
