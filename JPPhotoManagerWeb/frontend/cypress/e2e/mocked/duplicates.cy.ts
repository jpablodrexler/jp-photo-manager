import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Duplicates feature.

const makeAsset = (id: number, fileName: string) => ({
  assetId: id,
  folderId: 1,
  folderPath: '/photos',
  fileName,
  fileSize: 102400,
  thumbnailCreationDateTime: '2024-01-01T00:00:00',
  hash: 'samehash',
  thumbnailUrl: `/api/assets/${id}/thumbnail`,
  imageUrl: `/api/assets/${id}/image`,
  rating: 0,
  tags: [],
  fileType: 'IMAGE',
  isVideo: false,
});

describe('Duplicates', () => {
  it('duplicatesPage_pageLoaded_duplicateGroupsAreRendered', () => {
    cy.intercept('GET', '/api/assets/duplicates', {
      body: [[makeAsset(1, 'photo_a.jpg'), makeAsset(2, 'photo_a_copy.jpg')]],
    }).as('getDuplicates');

    visitWithSession('/duplicates');
    cy.wait('@getDuplicates');

    cy.get('.asset-item').should('have.length', 2);
  });
});
