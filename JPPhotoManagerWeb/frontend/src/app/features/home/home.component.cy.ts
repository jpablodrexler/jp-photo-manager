import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { HomeComponent } from './home.component';
import { HomeService } from '../../core/services/home.service';
import { HomeStats } from '../../core/models/home-stats.model';

describe('HomeComponent', () => {
  function mountHome(stats: HomeStats) {
    const homeServiceStub: Partial<HomeService> = {
      getStats: cy.stub().returns(of(stats))
    };
    return mount(HomeComponent, {
      providers: [
        { provide: HomeService, useValue: homeServiceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    });
  }

  it('ngOnInit_displaysFolderCount', () => {
    mountHome({ folderCount: 42, assetCount: 100, lastCatalogCompletedAt: '2024-06-01T10:00:00Z' });
    cy.contains('42').should('exist');
    cy.contains('Folders Catalogued').should('exist');
  });

  it('ngOnInit_displaysAssetCount', () => {
    mountHome({ folderCount: 10, assetCount: 999, lastCatalogCompletedAt: '2024-06-01T10:00:00Z' });
    cy.contains('999').should('exist');
    cy.contains('Assets Catalogued').should('exist');
  });

  it('ngOnInit_nullLastCompleted_displaysNever', () => {
    mountHome({ folderCount: 0, assetCount: 0, lastCatalogCompletedAt: null });
    cy.contains('Never').should('exist');
  });
});
