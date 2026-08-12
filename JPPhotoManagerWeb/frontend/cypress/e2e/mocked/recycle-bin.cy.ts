import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Recycle Bin feature.

const deletedAssets = [
  {
    assetId: 1, folderId: 1, folderPath: '/photos', fileName: 'sunset.jpg',
    fileSize: 1024000, thumbnailCreationDateTime: '2024-06-01T10:00:00',
    hash: 'abc123', thumbnailUrl: '/api/assets/1/thumbnail', imageUrl: '/api/assets/1/image',
    rating: 0, tags: [], fileType: 'IMAGE', isVideo: false,
  },
];

describe('Recycle bin', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/recycle-bin*', {
      body: { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 1 },
    }).as('getRecycleBin');

    visitWithSession('/recycle-bin');
    cy.wait('@getRecycleBin');
  });

  it('recycleBinPage_pageLoaded_deletedAssetsAreRendered', () => {
    cy.get('app-thumbnail').should('have.length', 1);
  });

  it('recycleBinPage_restoreClicked_showsSuccessSnackbar', () => {
    cy.intercept('POST', '/api/recycle-bin/restore', { statusCode: 204 }).as('restore');

    cy.get('app-thumbnail').first().click();
    cy.get('button').contains('Restore').click();
    cy.wait('@restore');

    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Restored successfully');
  });
});
