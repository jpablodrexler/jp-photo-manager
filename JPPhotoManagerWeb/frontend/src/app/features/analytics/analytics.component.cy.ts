import { mount } from 'cypress/angular';
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

  it('analyticsComponent_whenLoading_showsSpinner', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(NEVER),
    };

    mount(AnalyticsComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AnalyticsService, useValue: serviceStub },
      ],
    });

    cy.get('mat-spinner').should('exist');
    cy.get('.analytics-grid').should('not.exist');
  });

  it('analyticsComponent_onSuccess_rendersFourCharts', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(of(mockData)),
    };

    mount(AnalyticsComponent, {
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

  it('analyticsComponent_onError_showsErrorMessage', () => {
    const serviceStub: Partial<AnalyticsService> = {
      getAnalytics: cy.stub().returns(throwError(() => new Error('API error'))),
    };

    mount(AnalyticsComponent, {
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
