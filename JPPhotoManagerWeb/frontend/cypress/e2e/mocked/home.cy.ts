import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Home dashboard.

describe('Home', () => {
  it('homePage_statsLoaded_summaryCardsAreVisible', () => {
    cy.intercept('GET', '/api/home/stats', {
      body: {
        folderCount: 5,
        assetCount: 150,
        lastCatalogCompletedAt: '2024-06-01T10:00:00Z',
        totalFileSize: 26_112_000_000,
        duplicateCount: 4,
        topFolders: [
          { path: '/photos/vacation', assetCount: 100 },
          { path: '/photos/family', assetCount: 80 },
        ],
        recentAssets: [
          {
            assetId: 1, fileName: 'photo1.jpg', folderPath: '/photos/vacation',
            thumbnailUrl: '/api/assets/1/thumbnail', fileSize: 1_048_576,
          },
        ],
      },
    }).as('getStats');

    visitWithSession('/home');
    cy.wait('@getStats');

    cy.get('.folder-row').should('have.length', 2);
    cy.get('.strip-item app-thumbnail').should('have.length', 1);
  });
});
