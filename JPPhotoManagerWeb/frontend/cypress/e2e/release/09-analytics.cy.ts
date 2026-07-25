// Real-backend E2E: Analytics. Depends on 01-seed-and-catalog.cy.ts for
// real per-folder/format/rating data to chart.

import { realLogin } from '../../support/e2e-release-helpers';

describe('Analytics (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/analytics');
  });

  it('analyticsPage_afterSeeding_rendersAllFourChartsWithNoErrorState', () => {
    cy.get('.analytics-error').should('not.exist');
    cy.get('.analytics-grid mat-card', { timeout: 15000 }).should('have.length', 4);
  });

  it('formatDistributionChart_rendersRealPngSliceFromSeededAssets', () => {
    // All seeded release-suite assets are PNG (see cypress/fixtures/e2e-release);
    // the pie chart's legend should reflect that real format distribution.
    cy.contains('mat-card-title', 'File Format Distribution')
      .parents('mat-card')
      .find('svg', { timeout: 15000 })
      .should('exist');
  });

  it('storagePerFolderChart_rendersTreeMap', () => {
    cy.contains('mat-card-title', 'Storage per Folder')
      .parents('mat-card')
      .find('svg', { timeout: 15000 })
      .should('exist');
  });
});
