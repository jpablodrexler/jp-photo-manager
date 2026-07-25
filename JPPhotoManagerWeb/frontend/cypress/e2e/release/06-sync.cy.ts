// Real-backend E2E: Sync. Depends on 01-seed-and-catalog.cy.ts for
// /e2e-catalog/trip's 3 real files to sync from.

import { realLogin, TRIP_FOLDER, SYNCED_FOLDER } from '../../support/e2e-release-helpers';

describe('Sync (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/sync');
  });

  it('sync_tripToSyncedFolder_copiesAllThreeRealFiles', () => {
    cy.contains('button', 'Add').click();

    // The mat-table row has one input per column, in matColumnDef order:
    // sourceDirectory, destinationDirectory, includeSubFolders, deleteAssetsNotInSource, actions.
    cy.get('tbody tr').should('have.length', 1);
    cy.get('tbody tr input').eq(0).type(TRIP_FOLDER);
    cy.get('tbody tr input').eq(1).type(SYNCED_FOLDER);

    cy.contains('button', 'Save & Run').click();

    cy.contains('Results', { timeout: 60000 }).should('exist');
    cy.get('.result-item').should('have.length', 1);
    cy.get('.result-item').should('contain.text', 'Copied: 3');
    cy.get('.result-item.result-error').should('not.exist');
  });
});
