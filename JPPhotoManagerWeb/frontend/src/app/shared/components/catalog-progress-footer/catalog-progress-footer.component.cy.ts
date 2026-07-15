import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CatalogProgressFooterComponent } from './catalog-progress-footer.component';

describe('CatalogProgressFooterComponent', () => {
  it('should create the component', () => {
    cy.mount(CatalogProgressFooterComponent, {
      providers: [provideNoopAnimations()],
    }).then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should render the idle state by default', () => {
    cy.mount(CatalogProgressFooterComponent, {
      providers: [provideNoopAnimations()],
    });

    cy.get('.catalog-status-text').should('contain.text', 'Idle');
    cy.get('mat-progress-bar').should('not.exist');
  });

  it('should show the progress bar and percent when running', () => {
    cy.mount(CatalogProgressFooterComponent, {
      componentProperties: {
        state: 'running',
        percentCompleted: 60,
        currentStatusText: '/photos/vacation',
      },
      providers: [provideNoopAnimations()],
    });

    cy.get('mat-progress-bar').should('exist');
    cy.get('.catalog-percent').should('contain.text', '60%');
    cy.get('.catalog-status-text').should('contain.text', '/photos/vacation');
  });

  it('should display the last completed timestamp when idle', () => {
    cy.mount(CatalogProgressFooterComponent, {
      componentProperties: { lastCompletedAt: new Date() },
      providers: [provideNoopAnimations()],
    });

    cy.get('.catalog-status-text').should('contain.text', 'Idle');
  });

  it('should emit reconnect when the refresh button is clicked', () => {
    cy.mount(CatalogProgressFooterComponent, {
      providers: [provideNoopAnimations()],
    }).then(({ fixture }) => {
      cy.spy(fixture.componentInstance.reconnect, 'emit').as('reconnectSpy');
      cy.get('.catalog-refresh-btn').click();
      cy.get('@reconnectSpy').should('have.been.calledOnce');
    });
  });

  it('should disable the refresh button while running', () => {
    cy.mount(CatalogProgressFooterComponent, {
      componentProperties: { state: 'running' },
      providers: [provideNoopAnimations()],
    });

    cy.get('.catalog-refresh-btn').should('be.disabled');
  });
});
