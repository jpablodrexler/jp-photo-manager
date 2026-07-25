// Real-backend E2E: Recycle Bin. Depends on 01-seed-and-catalog.cy.ts.
// Uses events/magenta-square.png specifically, since no earlier release
// spec touches it.

import { realLogin, visitGalleryFolder, EVENTS_FOLDER } from '../../support/e2e-release-helpers';

function softDeleteMagentaAsset(): void {
  visitGalleryFolder(EVENTS_FOLDER);
  cy.contains('.asset-list-row', 'magenta-square').click(); // select
  cy.get('button[title="Actions"]').click();
  cy.contains('button', 'Remove from catalog').click();
}

describe('Recycle Bin (real backend)', () => {
  beforeEach(() => {
    realLogin();
  });

  it('softDeletedAsset_appearsInRecycleBin_andIsGoneFromGallery', () => {
    softDeleteMagentaAsset();
    cy.contains('.asset-list-row', 'magenta-square').should('not.exist');

    cy.visit('/recycle-bin');
    cy.get('app-thumbnail', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('restoreFromRecycleBin_bringsAssetBackToGallery', () => {
    cy.visit('/recycle-bin');
    cy.get('.asset-cell', { timeout: 10000 }).first().click();
    cy.contains('button', 'Restore').click();
    cy.get('.asset-cell').should('have.length', 0);

    visitGalleryFolder(EVENTS_FOLDER);
    cy.contains('.asset-list-row', 'magenta-square', { timeout: 10000 }).should('exist');
  });

  it('purgePermanently_removesAssetFromRecycleBinForGood', () => {
    softDeleteMagentaAsset();

    cy.visit('/recycle-bin');
    cy.get('.asset-cell', { timeout: 10000 }).first().click();
    cy.contains('button', 'Delete Permanently').click();
    cy.get('.asset-cell').should('have.length', 0);
    cy.contains('Recycle bin is empty.').should('exist');
  });
});
