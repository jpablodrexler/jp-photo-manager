import { mount } from 'cypress/angular';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(() => {
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
      ],
    });
  });

  it('should create the app', () => {
    cy.mount(AppComponent, {
      providers: [provideRouter([]), provideNoopAnimations()],
    }).then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should render the navigation toolbar', () => {
    cy.get('mat-toolbar').should('exist');
  });

  it('should display navigation links', () => {
    cy.get('[routerLink]').should('have.length.greaterThan', 0);
  });
});
