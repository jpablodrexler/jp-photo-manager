import { E2E_PREFIX, uniqueSuffix } from '../support/commands';

// Real-backend E2E for Albums - full CRUD with zero photos, which is all
// that's needed since CreateAlbumUseCaseImpl only requires a userId (no
// asset reference). Gallery/asset-attachment coverage needs real catalogued
// photo fixtures and is deliberately out of scope here - see the e2e-suite
// skill's "what this suite doesn't cover" section.
//
// Tests share state deliberately (same album, created in test 1) - a real
// user session carried across a spec file rather than fully isolated
// tests, per this project's e2e-suite skill (§5, "Why sequential, not
// parallel").

describe('Albums', () => {
  const albumName = `${E2E_PREFIX} album ${uniqueSuffix()}`;
  let albumId: string | undefined;

  beforeEach(() => {
    cy.login();
    cy.visit('/albums');
  });

  after(() => {
    // Safety net only - the last test deletes the album via the real UI and
    // clears albumId once that succeeds. Runs if an earlier test failed
    // before reaching the delete step.
    if (albumId) {
      cy.request({ method: 'DELETE', url: `/api/albums/${albumId}`, failOnStatusCode: false });
    }
  });

  it('albumsPage_createAlbum_appearsInListWithZeroPhotos', () => {
    cy.get('button[title="New album"]').click();
    cy.get('.create-form input').first().type(albumName);
    cy.contains('.create-form-actions button', 'Create').click();

    cy.contains('mat-card.album-card', albumName)
      .should('be.visible')
      .within(() => {
        cy.contains('0 photos').should('exist');
      });

    cy.request('/api/albums').then((response) => {
      const created = (response.body as Array<{ albumId: string; name: string }>).find(
        (album) => album.name === albumName,
      );
      expect(created, 'created album present in GET /api/albums').to.exist;
      albumId = created?.albumId;
    });
  });

  it('albumsPage_openAlbum_detailPageShowsEmptyState', () => {
    cy.contains('mat-card.album-card', albumName).contains('a', 'Open').click();

    cy.location('pathname').should('match', /^\/albums\/.+/);
    cy.contains('No photos in this album.').should('be.visible');

    cy.get('button[title="Back to albums"]').click();
    cy.location('pathname').should('eq', '/albums');
  });

  it('albumsPage_deleteAlbum_removedFromList', () => {
    cy.contains('mat-card.album-card', albumName).find('button[title="Delete album"]').click();
    cy.contains('mat-card.album-card', albumName).should('not.exist');
    albumId = undefined;
  });
});
