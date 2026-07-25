// Real-backend E2E: Duplicates. Depends on 01-seed-and-catalog.cy.ts —
// trip/red-square.png and events/red-square-duplicate.png are byte-identical
// (same SHA-256 hash), so exactly one duplicate group of 2 is expected.

import { realLogin } from '../../support/e2e-release-helpers';

describe('Duplicates (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/duplicates');
  });

  it('duplicatesPage_afterSeeding_findsExactlyOneGroupOfTwo', () => {
    cy.get('.group-card', { timeout: 15000 }).should('have.length', 1);
    cy.get('.group-card').first().should('contain.text', '2 files');
    cy.get('.group-card .asset-item').should('have.length', 2);
  });

  it('duplicatesPage_deleteGroup_removesTheNonKeptCopyOnly', () => {
    cy.get('.group-card', { timeout: 15000 }).first().within(() => {
      cy.contains('button', 'Delete duplicates').click();
    });
    cy.get('.group-card').should('have.length', 0);
    cy.contains('No duplicate images found.').should('exist');

    // The kept copy must still exist somewhere in the catalog afterward —
    // confirms this deleted only the redundant file, not both.
    cy.request('/api/assets?folderPath=%2Fe2e-catalog%2Ftrip&page=0&sort=FILE_NAME')
      .its('body.items')
      .then((items: Array<{ fileName: string }>) => {
        expect(items.some((a) => a.fileName === 'red-square.png')).to.be.true;
      });
  });
});
