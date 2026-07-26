// Real-backend E2E: Sync. Depends on 01-seed-and-catalog.cy.ts for
// /e2e-catalog/trip's real files to sync from.

import { realLogin, TRIP_FOLDER, SYNCED_FOLDER } from '../../support/e2e-release-helpers';

describe('Sync (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/sync');
  });

  it('sync_tripToSyncedFolder_copiesAllRealFiles', () => {
    // trip starts with 3 seeded fixtures, but 04-duplicates.cy.ts (which runs before this spec)
    // permanently deletes whichever of the two byte-identical red-square copies the backend's
    // duplicate-group query didn't keep - deterministic on any given run, but not guaranteed to
    // always be the events copy (confirmed: it can just as easily keep events/red-square-duplicate.png
    // and delete trip/red-square.png instead). Read the real current count instead of assuming 3.
    cy.request(`/api/assets?folderPath=${encodeURIComponent(TRIP_FOLDER)}&page=0&sort=FILE_NAME`)
      .its('body.totalItems')
      .then((tripFileCount: number) => {
        cy.contains('button', 'Add').click();

        // The mat-table row has one input per column, in matColumnDef order:
        // sourceDirectory, destinationDirectory, includeSubFolders, deleteAssetsNotInSource, actions.
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr input').eq(0).type(TRIP_FOLDER);
        cy.get('tbody tr input').eq(1).type(SYNCED_FOLDER);

        cy.contains('button', 'Save & Run').click();

        cy.contains('Results', { timeout: 60000 }).should('exist');
        cy.get('.result-item').should('have.length', 1);
        cy.get('.result-item').should('contain.text', `Copied: ${tripFileCount}`);
        cy.get('.result-item.result-error').should('not.exist');
      });
  });
});
