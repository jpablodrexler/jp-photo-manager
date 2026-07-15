import { of, throwError, NEVER } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AnalyticsComponent } from './analytics.component';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsData } from '../../core/models/analytics.model';

describe('AnalyticsComponent', () => {
  const mockData: AnalyticsData = {
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
  };

  it('should show a spinner while analytics data is loading', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(NEVER),
    };

    cy.mount(AnalyticsComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AnalyticsService, useValue: serviceStub },
      ],
    });

    cy.get('mat-spinner').should('exist');
    cy.get('.analytics-grid').should('not.exist');
  });

  it('should render four charts when analytics data loads successfully', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(of(mockData)),
    };

    cy.mount(AnalyticsComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AnalyticsService, useValue: serviceStub },
      ],
    });

    cy.get('mat-spinner').should('not.exist');
    cy.get('.analytics-grid mat-card').should('have.length', 4);
    cy.contains('mat-card-title', 'Storage per Folder').should('exist');
    cy.contains('mat-card-title', 'File Format Distribution').should('exist');
    cy.contains('mat-card-title', 'Photos per Month').should('exist');
    cy.contains('mat-card-title', 'Rating Distribution').should('exist');
  });

  it('should show an error message when loading analytics data fails', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(throwError(() => new Error('API error'))),
    };

    cy.mount(AnalyticsComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AnalyticsService, useValue: serviceStub },
      ],
    });

    cy.get('mat-spinner').should('not.exist');
    cy.get('.analytics-grid').should('not.exist');
    cy.get('.analytics-error').should('exist');
    cy.get('mat-icon[color="warn"]').should('exist');
  });
});
