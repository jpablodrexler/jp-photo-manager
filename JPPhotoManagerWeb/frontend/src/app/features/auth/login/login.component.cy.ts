import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({ standalone: true, template: '' })
class HomeStubComponent {}

describe('LoginComponent', () => {
  function mountLogin(authServiceOverrides: Partial<AuthService> = {}) {
    const authServiceStub: Partial<AuthService> = {
      login: cy.stub().returns(of(undefined)),
      isLoggedIn: cy.stub().returns(false),
      ...authServiceOverrides
    };
    return cy.mount(LoginComponent, {
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        provideNoopAnimations(),
        provideRouter([{ path: 'home', component: HomeStubComponent }])
      ]
    }).then(({ component }) => ({ component, authServiceStub }));
  }

  it('should call AuthService.login with the entered credentials on submit', () => {
    mountLogin().then(({ authServiceStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(authServiceStub.login).should('have.been.calledWith', 'admin', 'admin');
    });
  });

  it('should display an error message when the server rejects the login', () => {
    mountLogin({
      login: cy.stub().returns(throwError(() => new Error('Unauthorized')))
    });
    cy.get('input[formControlName="username"]').type('admin');
    cy.get('input[formControlName="password"]').type('wrong');
    cy.get('button[type="submit"]').click();
    cy.get('.error-message').should('contain', 'Invalid username or password');
  });

  function mountLoginWithReturnUrl(returnUrl: string | null) {
    const authServiceStub: Partial<AuthService> = {
      login: cy.stub().returns(of(undefined)),
      isLoggedIn: cy.stub().returns(false)
    };
    const navigateByUrlStub = cy.stub();
    return cy.mount(LoginComponent, {
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: { navigateByUrl: navigateByUrlStub } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } }
        },
        provideNoopAnimations()
      ]
    }).then(() => ({ navigateByUrlStub }));
  }

  it('should navigate to a relative returnUrl after login', () => {
    mountLoginWithReturnUrl('/gallery').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/gallery');
    });
  });

  it('should fall back to home when the returnUrl is protocol-relative', () => {
    mountLoginWithReturnUrl('//evil.com').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/home');
    });
  });

  it('should fall back to home when the returnUrl is an absolute URL', () => {
    mountLoginWithReturnUrl('https://evil.com').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/home');
    });
  });
});
