import { mount } from 'cypress/angular';
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
    return mount(LoginComponent, {
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        provideNoopAnimations(),
        provideRouter([{ path: 'home', component: HomeStubComponent }])
      ]
    }).then(({ component }) => ({ component, authServiceStub }));
  }

  it('submit_validCredentials_callsAuthServiceLogin', () => {
    mountLogin().then(({ authServiceStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(authServiceStub.login).should('have.been.calledWith', 'admin', 'admin');
    });
  });

  it('login_serverError_displaysErrorMessage', () => {
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
    return mount(LoginComponent, {
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

  it('submit_relativeReturnUrl_navigatesToIt', () => {
    mountLoginWithReturnUrl('/gallery').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/gallery');
    });
  });

  it('submit_protocolRelativeReturnUrl_fallsBackToHome', () => {
    mountLoginWithReturnUrl('//evil.com').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/home');
    });
  });

  it('submit_absoluteReturnUrl_fallsBackToHome', () => {
    mountLoginWithReturnUrl('https://evil.com').then(({ navigateByUrlStub }) => {
      cy.get('input[formControlName="username"]').type('admin');
      cy.get('input[formControlName="password"]').type('admin');
      cy.get('button[type="submit"]').click();
      cy.wrap(navigateByUrlStub).should('have.been.calledWith', '/home');
    });
  });
});
