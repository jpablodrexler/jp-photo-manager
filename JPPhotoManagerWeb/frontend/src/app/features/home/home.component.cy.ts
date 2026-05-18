import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { HomeComponent } from './home.component';
import { HomeService } from '../../core/services/home.service';
import { HomeStats } from '../../core/models/home-stats.model';

describe('HomeComponent', () => {
  const emptyStats: HomeStats = {
    folderCount: 0,
    assetCount: 0,
    lastCatalogCompletedAt: null,
    totalFileSize: 0,
    duplicateCount: 0,
    topFolders: [],
    recentAssets: [],
  };

  const richStats: HomeStats = {
    folderCount: 5,
    assetCount: 150,
    lastCatalogCompletedAt: '2024-06-01T10:00:00Z',
    totalFileSize: 26_112_000_000,
    duplicateCount: 4,
    topFolders: [
      { path: '/photos/vacation', assetCount: 100 },
      { path: '/photos/family', assetCount: 80 },
      { path: '/photos/work', assetCount: 60 },
      { path: '/photos/nature', assetCount: 40 },
      { path: '/photos/misc', assetCount: 20 },
    ],
    recentAssets: Array.from({ length: 12 }, (_, i) => ({
      assetId: i + 1,
      fileName: `photo${i + 1}.jpg`,
      folderPath: '/photos/vacation',
      thumbnailUrl: `/api/assets/${i + 1}/thumbnail`,
    })),
  };

  function mountHome(stats: HomeStats) {
    const homeServiceStub: Partial<HomeService> = {
      getStats: cy.stub().returns(of(stats))
    };
    return mount(HomeComponent, {
      providers: [
        { provide: HomeService, useValue: homeServiceStub },
        provideNoopAnimations(),
        provideRouter([]),
      ]
    });
  }

  it('ngOnInit_displaysFolderCount', () => {
    mountHome({ ...emptyStats, folderCount: 42, assetCount: 100 });
    cy.contains('42').should('exist');
    cy.contains('Folders Catalogued').should('exist');
  });

  it('ngOnInit_displaysAssetCount', () => {
    mountHome({ ...emptyStats, folderCount: 10, assetCount: 999 });
    cy.contains('999').should('exist');
    cy.contains('Assets Catalogued').should('exist');
  });

  it('ngOnInit_nullLastCompleted_displaysNever', () => {
    mountHome(emptyStats);
    cy.contains('Never').should('exist');
  });

  // --- Task 8.1: Enriched stats ---

  it('richStats_totalSizeCardVisible', () => {
    mountHome(richStats);
    cy.contains('Total Size').should('be.visible');
  });

  it('richStats_duplicatesBadgeVisible_whenDuplicatesExist', () => {
    mountHome(richStats);
    cy.get('.mat-badge-content').should('exist');
    cy.get('.mat-badge-content').should('not.have.class', 'mat-badge-hidden');
  });

  it('richStats_recentPhotosStrip_hasTwelveThumbnails', () => {
    mountHome(richStats);
    cy.get('.strip-item app-thumbnail').should('have.length', 12);
  });

  it('richStats_topFoldersList_hasFiveRows', () => {
    mountHome(richStats);
    cy.get('.folder-row').should('have.length', 5);
  });

  it('richStats_topFolders_showsFolderPathAndCount', () => {
    mountHome(richStats);
    cy.get('.folder-row').first().within(() => {
      cy.get('.folder-path').should('contain', '/photos/vacation');
      cy.get('.folder-count').should('contain', '100');
    });
  });

  // --- Task 8.2: Empty library ---

  it('emptyStats_recentPhotosStrip_isHiddenAndShowsEmptyState', () => {
    mountHome(emptyStats);
    cy.get('.strip-item').should('not.exist');
    cy.get('.empty-state').should('be.visible');
  });

  it('emptyStats_topFolders_sectionNotRendered', () => {
    mountHome(emptyStats);
    cy.get('.folder-row').should('not.exist');
  });

  it('emptyStats_totalSizeCard_showsZero', () => {
    mountHome(emptyStats);
    cy.get('.stat-card').contains('Total Size').parents('.stat-card').within(() => {
      cy.get('.stat-value').should('contain', '0 B');
    });
  });

  it('emptyStats_duplicatesBadge_isHidden', () => {
    mountHome(emptyStats);
    cy.get('.mat-badge-hidden').should('exist');
  });

  // --- Task 8.3: Click thumbnail navigates to gallery ---

  it('clickRecentPhoto_navigatesToGalleryWithFolderParam', () => {
    const navigateSpy = cy.stub().as('navigate');

    const homeServiceStub: Partial<HomeService> = {
      getStats: cy.stub().returns(of(richStats))
    };

    mount(HomeComponent, {
      providers: [
        { provide: HomeService, useValue: homeServiceStub },
        provideNoopAnimations(),
        provideRouter([]),
      ]
    }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      cy.stub(component['router'], 'navigate').as('routerNavigate');
      fixture.detectChanges();
    });

    cy.get('.strip-item').first().click();
    cy.get('@routerNavigate').should('have.been.calledWith',
      ['/gallery'],
      { queryParams: { folder: '/photos/vacation' } }
    );
  });
});
