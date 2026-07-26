// Real-backend E2E: Home dashboard. Depends on 01-seed-and-catalog.cy.ts
// having run first in the same suite invocation.

import { realLogin } from '../../support/e2e-release-helpers';

describe('Home dashboard (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/home');
  });

  it('homePage_afterSeeding_statCardsReflectRealData', () => {
    cy.get('.stat-value').eq(0).invoke('text').then((folderCount) => {
      expect(Number(folderCount.trim())).to.be.greaterThan(0);
    });
    cy.get('.stat-value').eq(1).invoke('text').then((assetCount) => {
      expect(Number(assetCount.trim())).to.be.at.least(6);
    });
  });

  it('homePage_afterCatalogRun_lastCatalogCompletedIsNotNever', () => {
    cy.get('.stat-card').eq(2).find('.stat-value').should('not.contain.text', 'Never');
  });

  it('homePage_recentPhotosStrip_rendersRealThumbnails', () => {
    cy.get('.strip-item').should('have.length.greaterThan', 0);
    cy.get('.strip-item').first().find('img').should('have.attr', 'src').and('include', '/api/assets/');
  });

  it('homePage_clickingRecentPhoto_navigatesToGalleryScopedToItsFolder', () => {
    cy.get('.strip-item').first().click();
    cy.url().should('include', '/gallery?folder=');
  });

  // The Top Folders widget shows only the top 5 folders by asset count (see
  // HomeComponent/GetHomeStatsUseCaseImpl) - on a live deployment with substantial real personal
  // libraries, /e2e-catalog's 2-3 seeded assets per subfolder will never outrank real folders with
  // hundreds of files, so asserting e2e-catalog appears in this specific widget doesn't hold
  // (confirmed via release-e2e-suite history). The recent-photos-strip tests above already cover
  // the dashboard reflecting freshly seeded data; this one is scoped to what's actually achievable
  // regardless of how much real data coexists: the widget renders real folder rows at all.
  it('homePage_topFoldersList_rendersRealFolderRowsWithValidPaths', () => {
    cy.get('.folder-row .folder-path', { timeout: 15000 }).should('have.length.greaterThan', 0);
    cy.get('.folder-row .folder-path').each(($el) => {
      expect($el.attr('title')).to.match(/^\//);
    });
  });
});
