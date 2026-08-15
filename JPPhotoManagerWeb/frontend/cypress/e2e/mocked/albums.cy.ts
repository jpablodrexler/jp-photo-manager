import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Albums feature, covering both the list (/albums)
// and navigating into a single album's detail (/albums/:id).

describe('Albums', () => {
  it('albumsPage_pageLoaded_albumCardsAreRendered', () => {
    cy.intercept('GET', '/api/albums', {
      body: [
        { albumId: 1, name: 'Wedding', description: null, assetCount: 42, createdAt: '2024-01-01T00:00:00Z' },
        { albumId: 2, name: 'Best of 2025', description: 'Favourites', assetCount: 7, createdAt: '2025-01-01T00:00:00Z' },
      ],
    }).as('getAlbums');

    visitWithSession('/albums');
    cy.wait('@getAlbums');

    cy.get('mat-card').should('have.length', 2);
    cy.contains('Wedding').should('exist');
  });

  it('albumDetailPage_opened_assetsAreRendered', () => {
    cy.intercept('GET', '/api/albums', {
      body: [{ albumId: 1, name: 'Wedding', description: null, assetCount: 1, createdAt: '2024-01-01T00:00:00Z' }],
    }).as('getAlbums');
    cy.intercept('GET', '/api/albums/1*', {
      body: {
        albumId: 1,
        name: 'Wedding',
        description: null,
        createdAt: '2024-01-01T00:00:00Z',
        assets: {
          items: [
            {
              assetId: 101, folderId: 1, folderPath: '/photos', fileName: 'a.jpg',
              fileSize: 1000, thumbnailCreationDateTime: '2024-01-01T00:00:00', hash: 'h1',
              thumbnailUrl: '/api/assets/101/thumbnail', imageUrl: '/api/assets/101/image',
              rating: 0, tags: [], fileType: 'IMAGE', isVideo: false,
            },
          ],
          pageIndex: 0, totalPages: 1, totalItems: 1,
        },
      },
    }).as('getAlbum');

    visitWithSession('/albums');
    cy.wait('@getAlbums');
    cy.contains('a', 'Open').click();
    cy.wait('@getAlbum');

    cy.location('pathname').should('eq', '/albums/1');
    cy.get('app-thumbnail').should('have.length', 1);
    cy.get('mat-toolbar').contains('Wedding').should('exist');
  });
});
