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
      fileSize: 1_048_576,
    })),
  };

  function mountHome(stats: HomeStats) {
    const homeServiceStub: Partial<HomeService> = {
      getStats: cy.stub().returns(of(stats))
    };
    return cy.mount(HomeComponent, {
      providers: [
        { provide: HomeService, useValue: homeServiceStub },
        provideNoopAnimations(),
        provideRouter([]),
      ]
    });
  }

  it('should display the folder count on init', () => {
    mountHome({ ...emptyStats, folderCount: 42, assetCount: 100 });
    cy.contains('42').should('exist');
    cy.contains('Folders Catalogued').should('exist');
  });

  it('should display the asset count on init', () => {
    mountHome({ ...emptyStats, folderCount: 10, assetCount: 999 });
    cy.contains('999').should('exist');
    cy.contains('Assets Catalogued').should('exist');
  });

  it('should display "Never" when lastCompleted is null', () => {
    mountHome(emptyStats);
    cy.contains('Never').should('exist');
  });

  // --- Task 8.1: Enriched stats ---

  it('should show the total size card when stats are rich', () => {
    mountHome(richStats);
    cy.contains('Total Size').should('be.visible');
  });

  it('should show the duplicates badge when duplicates exist', () => {
    mountHome(richStats);
    cy.get('.mat-badge-content').should('exist');
    cy.get('.mat-badge-content').should('not.have.class', 'mat-badge-hidden');
  });

  it('should show twelve thumbnails in the recent photos strip', () => {
    mountHome(richStats);
    cy.get('.strip-item app-thumbnail').should('have.length', 12);
  });

  it('should display the file size on each thumbnail in the recent photos strip', () => {
    mountHome(richStats);
    cy.get('.strip-item app-thumbnail').first().within(() => {
      cy.get('.thumbnail-size').should('contain', 'MB');
    });
  });

  it('should show five rows in the top folders list', () => {
    mountHome(richStats);
    cy.get('.folder-row').should('have.length', 5);
  });

  it('should show the folder path and asset count for each top folder row', () => {
    mountHome(richStats);
    cy.get('.folder-row').first().within(() => {
      cy.get('.folder-path').should('contain', '/photos/vacation');
      cy.get('.folder-count').should('contain', '100');
    });
  });

  // --- Task 8.2: Empty library ---

  it('should hide the recent photos strip and show the empty state when there are no stats', () => {
    mountHome(emptyStats);
    cy.get('.strip-item').should('not.exist');
    cy.get('.empty-state').should('be.visible');
  });

  it('should not render the top folders section when there are no stats', () => {
    mountHome(emptyStats);
    cy.get('.folder-row').should('not.exist');
  });

  it('should show zero on the total size card when there are no stats', () => {
    mountHome(emptyStats);
    cy.get('.stat-card').contains('Total Size').parents('.stat-card').within(() => {
      cy.get('.stat-value').should('contain', '0 B');
    });
  });

  it('should hide the duplicates badge when there are no stats', () => {
    mountHome(emptyStats);
    cy.get('.mat-badge-hidden').should('exist');
  });

  // --- Task 8.3: Click thumbnail navigates to gallery ---

  it('should navigate to the gallery with the folder and assetId params when a recent photo is clicked', () => {
    const navigateSpy = cy.stub().as('navigate');

    const homeServiceStub: Partial<HomeService> = {
      getStats: cy.stub().returns(of(richStats))
    };

    cy.mount(HomeComponent, {
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
      { queryParams: { folder: '/photos/vacation', assetId: 1 } }
    );
  });
});
