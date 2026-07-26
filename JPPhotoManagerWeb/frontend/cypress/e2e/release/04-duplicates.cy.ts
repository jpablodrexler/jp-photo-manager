// Real-backend E2E: Duplicates. Depends on 01-seed-and-catalog.cy.ts —
// trip/red-square.png and events/red-square-duplicate.png are byte-identical
// (same SHA-256 hash), so a duplicate group of 2 containing them is expected.
//
// GET /api/assets/duplicates returns every duplicate group in the whole live deployment,
// not scoped to /e2e-catalog - the operator's real personal libraries (/catalog, /catalog2,
// /catalog3) can and do have their own real duplicate groups (confirmed via release-e2e-suite
// history: a run against a live deployment with real data found 215 real groups alongside this
// suite's own 1). Asserting "exactly 1 group total" doesn't hold on a live deployment, so every
// assertion below is scoped to the specific group containing our seeded fixtures instead.

import { realLogin } from '../../support/e2e-release-helpers';

describe('Duplicates (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/duplicates');
  });

  it('duplicatesPage_afterSeeding_findsGroupOfTwoForSeededFixtures', () => {
    cy.contains('.group-card', 'red-square.png', { timeout: 15000 }).as('seedGroup');
    cy.get('@seedGroup').should('contain.text', '2 files');
    cy.get('@seedGroup').should('contain.text', 'red-square-duplicate.png');
    cy.get('@seedGroup').find('.asset-item').should('have.length', 2);
  });

  it('duplicatesPage_deleteGroup_removesTheNonKeptCopyOnly', () => {
    cy.contains('.group-card', 'red-square.png', { timeout: 15000 }).within(() => {
      cy.contains('button', 'Delete duplicates').click();
    });
    // Once the redundant copy is gone, red-square.png is no longer part of any duplicate
    // group, so no .group-card should mention it anymore — unrelated real duplicate groups
    // elsewhere in the deployment are untouched and irrelevant to this assertion.
    cy.contains('.group-card', 'red-square.png').should('not.exist');

    // DuplicatesComponent defaults keepIndex to 0 - i.e. whichever asset the backend's
    // duplicate-group query happens to return first - which is not guaranteed to be
    // trip/red-square.png specifically (confirmed: it can just as easily keep
    // events/red-square-duplicate.png instead and delete the trip copy). Check across both
    // folders that exactly one of the two byte-identical fixtures survived, rather than
    // assuming which one.
    cy.request('/api/assets?folderPath=%2Fe2e-catalog%2Ftrip&page=0&sort=FILE_NAME')
      .its('body.items')
      .then((tripItems: Array<{ fileName: string }>) => {
        cy.request('/api/assets?folderPath=%2Fe2e-catalog%2Fevents&page=0&sort=FILE_NAME')
          .its('body.items')
          .then((eventsItems: Array<{ fileName: string }>) => {
            const survivors = [
              ...tripItems.filter((a) => a.fileName === 'red-square.png'),
              ...eventsItems.filter((a) => a.fileName === 'red-square-duplicate.png'),
            ];
            expect(survivors).to.have.length(1);
          });
      });
  });
});
