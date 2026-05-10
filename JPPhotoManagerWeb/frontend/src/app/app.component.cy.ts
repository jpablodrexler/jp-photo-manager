import { mount } from 'cypress/angular';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthService } from './core/services/auth.service';

describe('AppComponent', () => {
  function mountApp() {
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      logout: (() => {}) as unknown as () => void,
    };
    const bpObsStub: Partial<BreakpointObserver> = {
      observe: cy.stub().returns(of({ matches: false, breakpoints: {} })),
    };
    return cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: BreakpointObserver, useValue: bpObsStub },
      ],
    });
  }

  beforeEach(() => { mountApp(); });

  it('should create the app', () => {
    mountApp().then(({ fixture }) => {
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

describe('AppComponent — responsive navigation', () => {
  function mountApp(isMobileMatches: boolean) {
    const bpObs: Partial<BreakpointObserver> = {
      observe: cy.stub().returns(of({ matches: isMobileMatches, breakpoints: {} })),
    };
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      logout: cy.stub() as unknown as () => void,
    };

    return cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: BreakpointObserver, useValue: bpObs },
        { provide: AuthService, useValue: authServiceStub },
      ],
    });
  }

  it('mobileViewport_hamburgerButtonVisible_inlineLinksNotRendered', () => {
    mountApp(true);
    cy.get('button[aria-label="Open navigation"]').should('be.visible');
    cy.get('a[routerLink="/home"]').should('not.exist');
  });

  it('desktopViewport_inlineLinksVisible_hamburgerNotRendered', () => {
    mountApp(false);
    cy.get('button[aria-label="Open navigation"]').should('not.exist');
    cy.get('a[routerLink="/home"]').should('be.visible');
  });
});
