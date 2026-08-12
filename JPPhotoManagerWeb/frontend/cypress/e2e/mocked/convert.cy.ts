import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Convert feature.

describe('Convert', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/convert/configuration', {
      body: [
        {
          id: 1,
          sourceDirectory: '/photos/png',
          destinationDirectory: '/photos/jpeg',
          includeSubFolders: true,
          deleteAssetsNotInSource: false,
          order: 0,
        },
      ],
    }).as('getConfiguration');

    visitWithSession('/convert');
    cy.wait('@getConfiguration');
  });

  it('convertPage_pageLoaded_configurationRowIsRendered', () => {
    cy.get('tr[mat-row]').should('have.length', 1);
  });

  it('convertPage_addDefinitionClicked_newRowIsRendered', () => {
    cy.get('button').contains('Add').click();
    cy.get('tr[mat-row]').should('have.length', 2);
  });
});
