import { visitWithSession } from '../../support/mocked/seed-session';
import 'cypress-axe';

// Deep, per-route accessibility (a11y) audit — the complement to
// scripts/lighthouse-report.mjs's Lighthouse accessibility SCORE, which only
// ever covers /login (the one route reachable without an authenticated
// session). Lighthouse gives one aggregate number; this spec drives
// axe-core (via cypress-axe) against every authenticated route and records
// the actual WCAG rule violated, its impact level, the affected selector(s),
// and the rule's help URL — actionable detail Lighthouse's score alone can't
// provide.
//
// Runs with no live backend, the same way the mocked E2E smoke tier
// (cypress/e2e/mocked/**) does: visitWithSession() seeds the
// 'photomanager_session' localStorage key so authGuard lets every route
// through, and every /api/** call this spec's routes make is stubbed via
// cy.intercept, mirroring each route's own cypress/e2e/mocked/*.cy.ts spec.
//
// Deliberately lives outside both existing Cypress tiers (see
// cypress.config.ts's excludeSpecPattern and cypress.a11y.config.ts) so it
// never runs as part of `npm run test:e2e` or `npm run test:e2e:mocked` —
// this is a report-only quality metric, not a CI gate. It's driven instead
// by scripts/a11y-report.js (`npm run a11y:report`), which also tolerates a
// non-zero Cypress exit code as a legitimate finding rather than a crash.
//
// `cy.checkA11y(context, options, violationCallback, skipFailures)` is
// called with `skipFailures: true` everywhere below — this spec's job is to
// collect every violation it finds, not to fail the run the first time one
// turns up.

interface A11yViolationRecord {
  id: string;
  impact: string;
  description: string;
  help: string;
  helpUrl: string;
  nodeCount: number;
  targets: string[];
}

interface A11yRouteResult {
  route: string;
  violations: A11yViolationRecord[];
}

const results: A11yRouteResult[] = [];

// Collects the current page's axe-core violations into `results`, keyed by
// route. Called once per route, right after that route's data has finished
// loading (post cy.wait(...)) so axe scans the fully-rendered DOM rather
// than a loading-spinner placeholder.
function auditCurrentPage(route: string): void {
  cy.injectAxe();
  cy.checkA11y(
    undefined,
    undefined,
    (violations) => {
      results.push({
        route,
        violations: violations.map((v) => ({
          id: v.id,
          impact: v.impact ?? 'unknown',
          description: v.description,
          help: v.help,
          helpUrl: v.helpUrl,
          nodeCount: v.nodes.length,
          targets: v.nodes.map((n) => n.target.join(' ')),
        })),
      });
    },
    true,
  );
}

const mockAsset = {
  assetId: 1, folderId: 1, folderPath: '/photos', fileName: 'sunset.jpg',
  fileSize: 1024000, thumbnailCreationDateTime: '2024-06-01T10:00:00',
  hash: 'abc123', thumbnailUrl: '/api/assets/1/thumbnail',
  imageUrl: '/api/assets/1/image', rating: 0, tags: [], fileType: 'IMAGE', isVideo: false,
};

describe('Accessibility audit (authenticated routes)', () => {
  // Written once, after every route in this spec has been visited and
  // audited, rather than appended incrementally per-route — a single write
  // at the end avoids read/modify/write races between tests and keeps the
  // on-disk JSON's shape identical to the in-memory `results` array.
  after(() => {
    cy.writeFile('cypress/a11y-results/raw.json', JSON.stringify(results, null, 2));
  });

  it('homePage_a11yAudit', () => {
    cy.intercept('GET', '/api/home/stats', {
      body: {
        folderCount: 5,
        assetCount: 150,
        lastCatalogCompletedAt: '2024-06-01T10:00:00Z',
        totalFileSize: 26_112_000_000,
        duplicateCount: 4,
        topFolders: [{ path: '/photos/vacation', assetCount: 100 }],
        recentAssets: [mockAsset],
      },
    }).as('getStats');

    visitWithSession('/home');
    cy.wait('@getStats');
    auditCurrentPage('/home');
  });

  it('galleryPage_a11yAudit', () => {
    cy.intercept('GET', '/api/folders*', { body: [{ folderId: 1, path: '/photos', parentPath: null }] }).as('getFolders');
    cy.intercept('GET', '/api/folders/initial', { body: { path: '/photos' } }).as('getInitial');
    cy.intercept('GET', '/api/folders/drives', { body: [] }).as('getDrives');
    cy.intercept('GET', /\/api\/assets(\?|$)/, {
      body: { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 },
    }).as('getAssets');
    cy.intercept('GET', '/api/assets/*/thumbnail', { fixture: 'thumbnail.jpg' }).as('getThumbnail');
    cy.intercept('GET', '/api/albums*', { body: [] }).as('getAlbums');
    cy.intercept('GET', '/api/search-presets*', { body: [] }).as('getPresets');

    visitWithSession('/gallery');
    cy.get('mat-tree-node').first().should('be.visible').click();
    cy.wait('@getAssets');
    auditCurrentPage('/gallery');
  });

  it('syncPage_a11yAudit', () => {
    cy.intercept('GET', '/api/sync/configuration', {
      body: [{ id: 1, sourceDirectory: '/photos/source', destinationDirectory: '/photos/dest', includeSubFolders: true, deleteAssetsNotInSource: false, order: 0 }],
    }).as('getConfiguration');

    visitWithSession('/sync');
    cy.wait('@getConfiguration');
    auditCurrentPage('/sync');
  });

  it('convertPage_a11yAudit', () => {
    cy.intercept('GET', '/api/convert/configuration', {
      body: [{ id: 1, sourceDirectory: '/photos/png', destinationDirectory: '/photos/jpeg', includeSubFolders: true, deleteAssetsNotInSource: false, order: 0 }],
    }).as('getConfiguration');

    visitWithSession('/convert');
    cy.wait('@getConfiguration');
    auditCurrentPage('/convert');
  });

  it('duplicatesPage_a11yAudit', () => {
    cy.intercept('GET', '/api/assets/duplicates', {
      body: [[mockAsset, { ...mockAsset, assetId: 2, fileName: 'sunset_copy.jpg', thumbnailUrl: '/api/assets/2/thumbnail', imageUrl: '/api/assets/2/image' }]],
    }).as('getDuplicates');

    visitWithSession('/duplicates');
    cy.wait('@getDuplicates');
    auditCurrentPage('/duplicates');
  });

  it('adminUsersPage_a11yAudit', () => {
    cy.intercept('GET', '/api/admin/users', {
      body: [
        { id: '11111111-1111-1111-1111-111111111111', username: 'admin', createdAt: '2024-01-01T00:00:00Z' },
        { id: '22222222-2222-2222-2222-222222222222', username: 'alice', createdAt: '2024-02-01T00:00:00Z' },
      ],
    }).as('getUsers');

    visitWithSession('/admin/users', 'ADMIN');
    cy.wait('@getUsers');
    auditCurrentPage('/admin/users');
  });

  it('albumsPage_a11yAudit', () => {
    cy.intercept('GET', '/api/albums', {
      body: [{ albumId: 1, name: 'Wedding', description: null, assetCount: 42, createdAt: '2024-01-01T00:00:00Z' }],
    }).as('getAlbums');

    visitWithSession('/albums');
    cy.wait('@getAlbums');
    auditCurrentPage('/albums');
  });

  it('albumDetailPage_a11yAudit', () => {
    cy.intercept('GET', '/api/albums/1*', {
      body: {
        albumId: 1,
        name: 'Wedding',
        description: null,
        createdAt: '2024-01-01T00:00:00Z',
        assets: { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 },
      },
    }).as('getAlbum');

    visitWithSession('/albums/1');
    cy.wait('@getAlbum');
    auditCurrentPage('/albums/1');
  });

  it('recycleBinPage_a11yAudit', () => {
    cy.intercept('GET', '/api/recycle-bin*', {
      body: { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 },
    }).as('getRecycleBin');

    visitWithSession('/recycle-bin');
    cy.wait('@getRecycleBin');
    auditCurrentPage('/recycle-bin');
  });

  it('analyticsPage_a11yAudit', () => {
    cy.intercept('GET', '/api/analytics', {
      body: {
        folderStorage: [{ folderPath: '/photos/vacation', bytes: 6000000 }],
        formatDistribution: [{ extension: 'jpg', count: 250 }],
        photosPerMonth: [{ month: '2024-01', count: 10 }],
        ratingDistribution: [{ rating: 0, count: 100 }],
      },
    }).as('getAnalytics');

    visitWithSession('/analytics');
    cy.wait('@getAnalytics');
    cy.get('mat-spinner').should('not.exist');
    auditCurrentPage('/analytics');
  });

  it('profileSessionsPage_a11yAudit', () => {
    cy.intercept('GET', '/api/auth/sessions', {
      body: [
        { id: 1, deviceHint: 'Chrome on Windows', lastUsedAt: '2024-06-01T10:00:00Z', current: true },
        { id: 2, deviceHint: 'Mobile Safari', lastUsedAt: '2024-05-30T08:00:00Z', current: false },
      ],
    }).as('getSessions');

    visitWithSession('/profile/sessions');
    cy.wait('@getSessions');
    auditCurrentPage('/profile/sessions');
  });
});
