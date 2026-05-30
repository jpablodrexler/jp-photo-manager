import { mount } from 'cypress/angular';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { AppComponent } from './app.component';
import { AuthService } from './core/services/auth.service';
import { MediaPlayerService } from './core/services/media-player.service';

const noop = () => {};

const mediaPlayerStub: Partial<MediaPlayerService> = {
  currentTrack: signal(null),
  isPlaying: signal(false),
  isVideoPlaying: signal(false),
  isAudioFullscreen: signal(false),
  videoStreamUrl: signal(null),
  currentTime: signal(0),
  duration: signal(0),
  registerVideoElement: noop as unknown as (el: HTMLVideoElement | null) => void,
  prev: noop as unknown as MediaPlayerService['prev'],
  stop: noop as unknown as MediaPlayerService['stop'],
  togglePause: noop as unknown as MediaPlayerService['togglePause'],
  next: noop as unknown as MediaPlayerService['next'],
  seek: noop as unknown as MediaPlayerService['seek'],
};

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
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: BreakpointObserver, useValue: bpObsStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
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
        provideHttpClient(),
        { provide: BreakpointObserver, useValue: bpObs },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
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
