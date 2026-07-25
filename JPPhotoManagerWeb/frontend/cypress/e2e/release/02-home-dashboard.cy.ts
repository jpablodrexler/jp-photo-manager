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

  it('homePage_topFoldersList_includesE2eCatalogSubfolder', () => {
    cy.get('.folder-row .folder-path').should(($els) => {
      const paths = $els.toArray().map((el) => el.getAttribute('title') ?? '');
      expect(paths.some((p) => p.includes('/e2e-catalog'))).to.be.true;
    });
  });
});
