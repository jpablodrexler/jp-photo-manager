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
