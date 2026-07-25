// Real-backend E2E: Convert. Depends on 01-seed-and-catalog.cy.ts for
// /e2e-catalog/events's 3 real PNG files to convert.

import { realLogin, EVENTS_FOLDER, CONVERTED_FOLDER } from '../../support/e2e-release-helpers';

describe('Convert (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/convert');
  });

  it('convert_eventsToConvertedFolder_convertsAllThreeRealPngFiles', () => {
    cy.contains('button', 'Add').click();

    // matColumnDef order: sourceDirectory, destinationDirectory, includeSubFolders, actions.
    cy.get('tbody tr').should('have.length', 1);
    cy.get('tbody tr input').eq(0).type(EVENTS_FOLDER);
    cy.get('tbody tr input').eq(1).type(CONVERTED_FOLDER);

    cy.contains('button', 'Save & Run').click();

    cy.contains('Results', { timeout: 60000 }).should('exist');
    cy.get('.result-item').should('have.length', 1);
    cy.get('.result-item').should('contain.text', 'Converted: 3');
    cy.get('.result-item.result-error').should('not.exist');
  });
});
