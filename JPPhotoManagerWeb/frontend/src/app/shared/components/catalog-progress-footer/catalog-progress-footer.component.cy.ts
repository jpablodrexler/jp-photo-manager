import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CatalogProgressFooterComponent } from './catalog-progress-footer.component';

function mountFooter(componentProperties: Partial<CatalogProgressFooterComponent> = {}) {
  return cy.mount(CatalogProgressFooterComponent, {
    componentProperties,
    providers: [provideNoopAnimations()],
  });
}

describe('CatalogProgressFooterComponent', () => {
  it('should create the component', () => {
    mountFooter().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should render the idle state by default', () => {
    mountFooter();

    cy.get('.catalog-status-text').should('contain.text', 'Idle');
    cy.get('mat-progress-bar').should('not.exist');
  });

  it('should show the progress bar and percent when running', () => {
    mountFooter({
      state: 'running',
      percentCompleted: 60,
      currentStatusText: '/photos/vacation',
    });

    cy.get('mat-progress-bar').should('exist');
    cy.get('.catalog-percent').should('contain.text', '60%');
    cy.get('.catalog-status-text').should('contain.text', '/photos/vacation');
  });

  it('should display the last completed timestamp when idle', () => {
    mountFooter({ lastCompletedAt: new Date() });

    cy.get('.catalog-status-text').should('contain.text', 'Idle');
  });

  it('should emit reconnect when the refresh button is clicked', () => {
    mountFooter().then(({ fixture }) => {
      cy.spy(fixture.componentInstance.reconnect, 'emit').as('reconnectSpy');
      cy.get('.catalog-refresh-btn').click();
      cy.get('@reconnectSpy').should('have.been.calledOnce');
    });
  });

  it('should disable the refresh button while running', () => {
    mountFooter({ state: 'running' });

    cy.get('.catalog-refresh-btn').should('be.disabled');
  });
});
