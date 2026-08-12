import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Analytics feature.

describe('Analytics', () => {
  it('analyticsPage_dataLoaded_summaryCardsAreRendered', () => {
    cy.intercept('GET', '/api/analytics', {
      body: {
        folderStorage: [
          { folderPath: '/photos/vacation', bytes: 6000000 },
          { folderPath: '/photos/family', bytes: 3000000 },
        ],
        formatDistribution: [
          { extension: 'jpg', count: 250 },
          { extension: 'png', count: 30 },
        ],
        photosPerMonth: [
          { month: '2024-01', count: 10 },
          { month: '2024-02', count: 25 },
        ],
        ratingDistribution: [
          { rating: 0, count: 100 },
          { rating: 3, count: 50 },
        ],
      },
    }).as('getAnalytics');

    visitWithSession('/analytics');
    cy.wait('@getAnalytics');

    cy.get('mat-spinner').should('not.exist');
    cy.get('.analytics-grid mat-card').should('have.length', 4);
  });
});
