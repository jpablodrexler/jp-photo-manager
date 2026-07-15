import { mount } from 'cypress/angular';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of, Observable } from 'rxjs';
import { signal } from '@angular/core';
import { AppComponent } from './app.component';
import { AuthService } from './core/services/auth.service';
import { MediaPlayerService } from './core/services/media-player.service';
import { ThemeService } from './core/services/theme.service';
import { PreferenceService } from './core/services/preference.service';

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

function buildProviders(isLoggedIn: boolean, isDark: Observable<boolean>) {
  const authServiceStub: Partial<AuthService> = {
    isLoggedIn: () => isLoggedIn,
    isAdmin: cy.stub().returns(false),
    logout: () => of(undefined),
  };
  const bpObsStub: Partial<BreakpointObserver> = {
    observe: cy.stub().returns(of({ matches: false, breakpoints: {} })),
  };
  const themeServiceStub: Partial<ThemeService> = {
    isDark$: isDark,
    init: cy.stub() as unknown as ThemeService['init'],
    toggle: cy.stub().returns('light') as unknown as ThemeService['toggle'],
  };
  const preferenceServiceStub: Partial<PreferenceService> = {
    load: cy.stub().returns(of(undefined)) as unknown as PreferenceService['load'],
    save: cy.stub().returns(of(undefined)) as unknown as PreferenceService['save'],
  };
  return {
    providers: [
      provideRouter([]),
      provideNoopAnimations(),
      provideHttpClient(),
      { provide: AuthService, useValue: authServiceStub },
      { provide: BreakpointObserver, useValue: bpObsStub },
      { provide: MediaPlayerService, useValue: mediaPlayerStub },
      { provide: ThemeService, useValue: themeServiceStub },
      { provide: PreferenceService, useValue: preferenceServiceStub },
    ],
    themeServiceStub,
  };
}

describe('AppComponent', () => {
  function mountApp() {
    const { providers } = buildProviders(true, of(true));
    return cy.mount(AppComponent, { providers });
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
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(false),
      logout: cy.stub().returns(of(undefined)) as unknown as () => Observable<void>,
    };
    const bpObs: Partial<BreakpointObserver> = {
      observe: cy.stub().returns(of({ matches: isMobileMatches, breakpoints: {} })),
    };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(true),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('light') as unknown as ThemeService['toggle'],
    };
    const preferenceServiceStub: Partial<PreferenceService> = {
      load: cy.stub().returns(of(undefined)) as unknown as PreferenceService['load'],
      save: cy.stub().returns(of(undefined)) as unknown as PreferenceService['save'],
    };
    return cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: BreakpointObserver, useValue: bpObs },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
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

describe('AppComponent — theme toggle', () => {
  it('toggleButton_themeIsLight_showsDarkModeIcon', () => {
    const { providers } = buildProviders(true, of(false));
    cy.mount(AppComponent, { providers });
    cy.get('button[aria-label="Switch to dark mode"]').should('exist');
    cy.get('button[aria-label="Switch to dark mode"] mat-icon')
      .should('contain.text', 'dark_mode');
  });

  it('toggleButton_themeIsDark_showsLightModeIcon', () => {
    const { providers } = buildProviders(true, of(true));
    cy.mount(AppComponent, { providers });
    cy.get('button[aria-label="Switch to light mode"]').should('exist');
    cy.get('button[aria-label="Switch to light mode"] mat-icon')
      .should('contain.text', 'light_mode');
  });

  it('toggleButton_notLoggedIn_isAbsent', () => {
    const authServiceStub: Partial<AuthService> = { isLoggedIn: () => false, isAdmin: cy.stub().returns(false) };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(true),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('light') as unknown as ThemeService['toggle'],
    };
    const preferenceServiceStub: Partial<PreferenceService> = {
      load: cy.stub().returns(of(undefined)) as unknown as PreferenceService['load'],
      save: cy.stub().returns(of(undefined)) as unknown as PreferenceService['save'],
    };
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
      ],
    });
    cy.get('button[aria-label*="mode"]').should('not.exist');
  });

  it('toggleButton_clicked_callsThemeServiceToggle', () => {
    const toggleStub = cy.stub().returns('light');
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(true),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: toggleStub as unknown as ThemeService['toggle'],
    };
    const preferenceServiceStub: Partial<PreferenceService> = {
      load: cy.stub().returns(of(undefined)) as unknown as PreferenceService['load'],
      save: cy.stub().returns(of(undefined)) as unknown as PreferenceService['save'],
    };
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: { isLoggedIn: () => true, isAdmin: cy.stub().returns(false), logout: cy.stub() } },
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
      ],
    });
    cy.get('button[aria-label="Switch to light mode"]').click();
    cy.wrap(toggleStub).should('have.been.called');
  });
});
