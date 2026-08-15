import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Sync feature.

describe('Sync', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/sync/configuration', {
      body: [
        {
          id: 1,
          sourceDirectory: '/photos/source',
          destinationDirectory: '/photos/dest',
          includeSubFolders: true,
          deleteAssetsNotInSource: false,
          order: 0,
        },
      ],
    }).as('getConfiguration');

    visitWithSession('/sync');
    cy.wait('@getConfiguration');
  });

  it('syncPage_pageLoaded_configurationRowIsRendered', () => {
    cy.get('tr[mat-row]').should('have.length', 1);
  });

  it('syncPage_addDefinitionClicked_newRowIsRendered', () => {
    cy.get('button').contains('Add').click();
    cy.get('tr[mat-row]').should('have.length', 2);
  });
});
