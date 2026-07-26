// Real-backend E2E: Convert. Depends on 01-seed-and-catalog.cy.ts for
// /e2e-catalog/events's real PNG files to convert.

import { realLogin, EVENTS_FOLDER, CONVERTED_FOLDER } from '../../support/e2e-release-helpers';

describe('Convert (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/convert');
  });

  it('convert_eventsToConvertedFolder_convertsAllRealPngFiles', () => {
    // events starts with 3 seeded fixtures, but 04-duplicates.cy.ts (which runs before this spec)
    // permanently deletes whichever of the two byte-identical red-square copies the backend's
    // duplicate-group query didn't keep - not guaranteed to always be the trip copy (confirmed:
    // it can just as easily keep trip/red-square.png and delete events/red-square-duplicate.png
    // instead - see 06-sync.cy.ts's identical situation for /e2e-catalog/trip). Read the real
    // current count instead of assuming 3.
    cy.request(`/api/assets?folderPath=${encodeURIComponent(EVENTS_FOLDER)}&page=0&sort=FILE_NAME`)
      .its('body.totalItems')
      .then((eventsFileCount: number) => {
        cy.contains('button', 'Add').click();

        // matColumnDef order: sourceDirectory, destinationDirectory, includeSubFolders, actions.
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr input').eq(0).type(EVENTS_FOLDER);
        cy.get('tbody tr input').eq(1).type(CONVERTED_FOLDER);

        cy.contains('button', 'Save & Run').click();

        cy.contains('Results', { timeout: 60000 }).should('exist');
        cy.get('.result-item').should('have.length', 1);
        cy.get('.result-item').should('contain.text', `Converted: ${eventsFileCount}`);
        cy.get('.result-item.result-error').should('not.exist');
      });
  });
});
