// E2E tests for the Gallery feature.
// Prerequisites: the dev server must be running (`npm start`) and the backend
// must be reachable, OR the API calls must be intercepted below.
//
// Auth: the app checks localStorage for a valid session object on each navigation.
// We seed the key before each test so the authGuard lets us through.

const SESSION_KEY = 'photomanager_session';

function seedAuth() {
  window.localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ username: 'admin', expiresAt: Date.now() + 3_600_000 }),
  );
}

const mockFolders = [
  { folderId: 1, path: '/photos', parentPath: null },
];

const mockAssets = [
  {
    assetId: 1, folderId: 1, folderPath: '/photos', fileName: 'sunset.jpg',
    fileSize: 1024000, thumbnailCreationDateTime: '2024-06-01T10:00:00',
    hash: 'abc123', thumbnailUrl: '/api/assets/1/thumbnail',
    imageUrl: '/api/assets/1/image', rating: 0, tags: [], fileType: 'IMAGE',
  },
  {
    assetId: 2, folderId: 1, folderPath: '/photos', fileName: 'beach.jpg',
    fileSize: 512000, thumbnailCreationDateTime: '2024-06-02T10:00:00',
    hash: 'def456', thumbnailUrl: '/api/assets/2/thumbnail',
    imageUrl: '/api/assets/2/image', rating: 0, tags: [], fileType: 'IMAGE',
  },
];

describe('Gallery', () => {
  beforeEach(() => {
    // Seed auth so authGuard passes without a real login
    cy.visit('/', {
      onBeforeLoad(win) {
        win.localStorage.setItem(
          SESSION_KEY,
          JSON.stringify({ username: 'admin', expiresAt: Date.now() + 3_600_000 }),
        );
      },
    });

    // Intercept folder and asset API calls
    cy.intercept('GET', '/api/folders*', { body: mockFolders }).as('getFolders');
    cy.intercept('GET', '/api/folders/initial', { body: { path: '/photos' } }).as('getInitial');
    cy.intercept('GET', '/api/folders/drives', { body: [] }).as('getDrives');
    cy.intercept('GET', '/api/assets*', {
      body: { items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2 },
    }).as('getAssets');
    cy.intercept('GET', '/api/assets/*/thumbnail', { fixture: 'thumbnail.jpg' }).as('getThumbnail');
    cy.intercept('GET', '/api/albums*', { body: [] }).as('getAlbums');
    cy.intercept('GET', '/api/search-presets*', { body: [] }).as('getPresets');

    cy.visit('/gallery');
  });

  it('galleryPage_pageLoaded_folderTreeIsVisible', () => {
    cy.get('app-folder-nav').should('exist');
    cy.get('mat-tree').should('exist');
  });

  it('galleryPage_folderSelected_assetListRowsAreRendered', () => {
    // Click the first folder in the tree to trigger onFolderSelected
    cy.get('mat-tree-node').first().click();
    cy.wait('@getAssets');
    cy.get('.asset-list-row').should('have.length', 2);
  });

  it('galleryPage_folderSelected_thumbnailImagesAreRendered', () => {
    cy.get('mat-tree-node').first().click();
    cy.wait('@getAssets');
    cy.get('.asset-list-row img.list-thumb').should('have.length', 2);
  });

  it('galleryPage_folderSelected_thumbnailSrcPointsToApiEndpoint', () => {
    cy.get('mat-tree-node').first().click();
    cy.wait('@getAssets');
    cy.get('.asset-list-row img.list-thumb').eq(0)
      .should('have.attr', 'src', '/api/assets/1/thumbnail');
    cy.get('.asset-list-row img.list-thumb').eq(1)
      .should('have.attr', 'src', '/api/assets/2/thumbnail');
  });

  it('galleryPage_noFolderSelected_emptyStateNotVisible', () => {
    // Without selecting a folder no empty-state message should be shown
    cy.get('.empty-state').should('not.exist');
  });
});
