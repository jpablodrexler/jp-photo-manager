import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError, Observable } from 'rxjs';
import { signal } from '@angular/core';
import { AppComponent } from './app.component';
import { AuthService } from './core/services/auth.service';
import { AssetService } from './core/services/asset.service';
import { BackgroundSyncService } from './core/services/background-sync.service';
import { MediaPlayerService } from './core/services/media-player.service';
import { ThemeService } from './core/services/theme.service';
import { PreferenceService } from './core/services/preference.service';
import { MockEventSource } from '../../cypress/support/mock-event-source';

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
      provideHttpClient(withXhr()),
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

  let componentInstance: AppComponent | undefined;

  beforeEach(() => {
    componentInstance = undefined;
    mountApp().then(({ fixture }) => { componentInstance = fixture.componentInstance; });
  });

  it('should create the app', () => {
    cy.then(() => {
      expect(componentInstance).to.be.ok;
    });
  });

  it('should render the navigation toolbar', () => {
    cy.get('mat-toolbar').should('exist');
  });

  it('should display navigation links', () => {
    cy.get('[routerLink]').should('have.length.greaterThan', 0);
  });
});

describe('AppComponent', () => {
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
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: bpObs },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
      ],
    });
  }

  it('should show the hamburger button and hide inline links on a mobile viewport', () => {
    mountApp(true);
    cy.get('button[aria-label="Open navigation"]').should('be.visible');
    cy.get('a[routerLink="/home"]').should('not.exist');
  });

  it('should show inline links and hide the hamburger button on a desktop viewport', () => {
    mountApp(false);
    cy.get('button[aria-label="Open navigation"]').should('not.exist');
    cy.get('a[routerLink="/home"]').should('be.visible');
  });
});

describe('AppComponent', () => {
  it('should show the dark mode icon on the toggle button when the theme is light', () => {
    const { providers } = buildProviders(true, of(false));
    cy.mount(AppComponent, { providers });
    cy.get('button[aria-label="Switch to dark mode"]').should('exist');
    cy.get('button[aria-label="Switch to dark mode"] mat-icon')
      .should('contain.text', 'dark_mode');
  });

  it('should show the light mode icon on the toggle button when the theme is dark', () => {
    const { providers } = buildProviders(true, of(true));
    cy.mount(AppComponent, { providers });
    cy.get('button[aria-label="Switch to light mode"]').should('exist');
    cy.get('button[aria-label="Switch to light mode"] mat-icon')
      .should('contain.text', 'light_mode');
  });

  it('should not render the theme toggle button when the user is not logged in', () => {
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
        provideHttpClient(withXhr()),
        { provide: AuthService, useValue: authServiceStub },
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
      ],
    });
    cy.get('button[aria-label*="mode"]').should('not.exist');
  });

  it('should call ThemeService.toggle when the toggle button is clicked', () => {
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
        provideHttpClient(withXhr()),
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

describe('AppComponent — admin links', () => {
  function mountAsAdmin(isMobileMatches: boolean) {
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(true),
      logout: cy.stub().returns(of(undefined)) as unknown as () => Observable<void>,
    };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(false),
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
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: isMobileMatches, breakpoints: {} })) } },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: preferenceServiceStub },
      ],
    });
  }

  it('should render the Sync/Convert/Users links for an admin on desktop', () => {
    mountAsAdmin(false);
    cy.get('a[routerLink="/sync"]').should('exist');
    cy.get('a[routerLink="/convert"]').should('exist');
    cy.get('a[routerLink="/admin/users"]').should('exist');
  });

  it('should render the Sync/Convert/Users menu items for an admin on mobile', () => {
    mountAsAdmin(true);
    cy.get('button[aria-label="Open navigation"]').click();
    cy.get('button[routerLink="/sync"]').should('exist');
    cy.get('button[routerLink="/convert"]').should('exist');
    cy.get('button[routerLink="/admin/users"]').should('exist');
  });

  it('should not render admin-only links for a non-admin user', () => {
    const { providers } = buildProviders(true, of(false));
    cy.mount(AppComponent, { providers });
    cy.get('a[routerLink="/sync"]').should('not.exist');
    cy.get('a[routerLink="/admin/users"]').should('not.exist');
  });
});

describe('AppComponent — logout and about dialog', () => {
  it('should call AuthService.logout and navigate to /login on logout', () => {
    const logoutStub = cy.stub().returns(of(undefined));
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(false),
      logout: logoutStub as unknown as () => Observable<void>,
    };
    const { providers } = buildProviders(true, of(false));
    let router: Router | undefined;
    cy.mount(AppComponent, {
      providers: [
        ...providers.filter(p => (p as { provide?: unknown }).provide !== AuthService),
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).then(({ fixture }) => {
      router = fixture.debugElement.injector.get(Router);
      cy.stub(router, 'navigateByUrl').as('navigateByUrl');
    });

    cy.get('button').contains('Logout').click();
    cy.wrap(logoutStub).should('have.been.called');
    cy.get('@navigateByUrl').should('have.been.calledWith', '/login');
  });

  it('should open the About dialog when the About button is clicked', () => {
    const openStub = cy.stub();
    const { providers } = buildProviders(true, of(false));
    cy.mount(AppComponent, {
      providers: [...providers, { provide: MatDialog, useValue: { open: openStub } }],
    });

    cy.get('button[aria-label="About"]').click();
    cy.wrap(openStub).should('have.been.called');
  });
});

describe('AppComponent — toggleTheme preference persistence', () => {
  it('should save the new theme preference when logged in', () => {
    const saveStub = cy.stub().returns(of(undefined));
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(false),
      logout: cy.stub().returns(of(undefined)) as unknown as () => Observable<void>,
    };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(false),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('dark') as unknown as ThemeService['toggle'],
    };
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: { load: cy.stub().returns(of(undefined)), save: saveStub } },
      ],
    });

    cy.get('button[aria-label="Switch to dark mode"]').click();
    cy.wrap(saveStub).should('have.been.calledWith', 'dark');
  });

  it('should not save the theme preference when not logged in', () => {
    const saveStub = cy.stub().returns(of(undefined));
    const authServiceStub: Partial<AuthService> = { isLoggedIn: () => false, isAdmin: cy.stub().returns(false) };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(false),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('dark') as unknown as ThemeService['toggle'],
    };
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: { load: cy.stub().returns(of(undefined)), save: saveStub } },
      ],
    }).then(({ fixture }) => {
      // The theme toggle button is not rendered at all when logged out
      // (verified separately in another spec), so exercise the guarded
      // `if (this.authService.isLoggedIn())` branch inside toggleTheme()
      // directly.
      fixture.componentInstance.toggleTheme();
    });

    cy.get('button[aria-label*="mode"]').should('not.exist');
    cy.wrap(saveStub).should('not.have.been.called');
  });

  it('should show an error snackbar when saving the theme preference fails', () => {
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(false),
      logout: cy.stub().returns(of(undefined)) as unknown as () => Observable<void>,
    };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(false),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('dark') as unknown as ThemeService['toggle'],
    };
    cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        {
          provide: PreferenceService,
          useValue: { load: cy.stub().returns(of(undefined)), save: cy.stub().returns(throwError(() => new Error('save failed'))) },
        },
      ],
    });

    cy.get('button[aria-label="Switch to dark mode"]').click();
    cy.contains('Failed to save theme preference').should('exist');
  });
});

describe('AppComponent — catalog progress via SSE', () => {
  // observeCatalog is always a cy.stub() (never a plain arrow function) so
  // every test can synchronize on it below.
  function mountWithCatalog(observeCatalogStub: AssetService['observeCatalog'], backgroundSyncOverrides: Partial<BackgroundSyncService> = {}) {
    const authServiceStub: Partial<AuthService> = {
      isLoggedIn: () => true,
      isAdmin: cy.stub().returns(false),
      logout: cy.stub().returns(of(undefined)) as unknown as () => Observable<void>,
    };
    const themeServiceStub: Partial<ThemeService> = {
      isDark$: of(false),
      init: cy.stub() as unknown as ThemeService['init'],
      toggle: cy.stub().returns('light') as unknown as ThemeService['toggle'],
    };
    const assetServiceStub: Partial<AssetService> = {
      observeCatalog: observeCatalogStub,
    };
    const backgroundSyncStub: Partial<BackgroundSyncService> = {
      getPendingCount: cy.stub().resolves(0),
      replayQueue: cy.stub().resolves(undefined),
      ...backgroundSyncOverrides,
    };
    return cy.mount(AppComponent, {
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(withXhr()),
        { provide: BreakpointObserver, useValue: { observe: cy.stub().returns(of({ matches: false, breakpoints: {} })) } },
        { provide: AuthService, useValue: authServiceStub },
        { provide: AssetService, useValue: assetServiceStub },
        { provide: BackgroundSyncService, useValue: backgroundSyncStub },
        { provide: MediaPlayerService, useValue: mediaPlayerStub },
        { provide: ThemeService, useValue: themeServiceStub },
        { provide: PreferenceService, useValue: { load: cy.stub().returns(of(undefined)), save: cy.stub().returns(of(undefined)) } },
      ],
    });
  }

  // ngDoCheck (which calls connectCatalog() and attaches the SSE listeners)
  // isn't guaranteed to have run by the moment cy.mount()'s promise
  // resolves in this zone.js app — it can take one more zone-triggered
  // change-detection tick. Emitting an SSE event before that tick attaches
  // the listeners silently loses the event (MockEventSource doesn't
  // replay). Wait on the stub call (a retrying Cypress assertion) first so
  // every subsequent emit is guaranteed to land on an attached listener.
  function waitForCatalogConnected(observeCatalogStub: AssetService['observeCatalog']) {
    cy.wrap(observeCatalogStub).should('have.been.calledOnce');
  }

  it('should show the folder path when a catalog event carries folderPath', () => {
    const source = new MockEventSource();
    const observeCatalogStub = cy.stub().returns(source) as unknown as AssetService['observeCatalog'];
    mountWithCatalog(observeCatalogStub);
    waitForCatalogConnected(observeCatalogStub);

    cy.then(() => {
      source.emit('catalog', { percentCompleted: 40, folderPath: '/photos/2026' });
    });
    cy.contains('/photos/2026').should('exist');
  });

  it('should show the asset file name when a catalog event has no folderPath', () => {
    const source = new MockEventSource();
    const observeCatalogStub = cy.stub().returns(source) as unknown as AssetService['observeCatalog'];
    mountWithCatalog(observeCatalogStub);
    waitForCatalogConnected(observeCatalogStub);

    cy.then(() => {
      source.emit('catalog', { percentCompleted: 60, asset: { fileName: 'sunset.jpg' } });
    });
    cy.contains('sunset.jpg').should('exist');
  });

  it('should reset catalog state to idle and record completion time on catalog-done', () => {
    const source = new MockEventSource();
    const observeCatalogStub = cy.stub().returns(source) as unknown as AssetService['observeCatalog'];
    mountWithCatalog(observeCatalogStub).then(({ fixture }) => {
      waitForCatalogConnected(observeCatalogStub);
      cy.then(() => {
        source.emit('catalog', { percentCompleted: 50, folderPath: '/x' });
        fixture.detectChanges();
        source.emit('catalog-done', {});
        fixture.detectChanges();
        expect(fixture.componentInstance.catalogState()).to.equal('idle');
        expect(fixture.componentInstance.catalogLastCompletedAt()).to.not.be.null;
      });
    });
  });

  it('should reset state to idle on error only while a catalog run is in progress', () => {
    const source = new MockEventSource();
    const observeCatalogStub = cy.stub().returns(source) as unknown as AssetService['observeCatalog'];
    mountWithCatalog(observeCatalogStub).then(({ fixture }) => {
      waitForCatalogConnected(observeCatalogStub);
      cy.then(() => {
        source.emit('catalog', { percentCompleted: 10, folderPath: '/x' });
        fixture.detectChanges();
        expect(fixture.componentInstance.catalogState()).to.equal('running');
        (source.onerror as unknown as ((ev: Event) => void) | null)?.(new Event('error'));
        fixture.detectChanges();
        expect(fixture.componentInstance.catalogState()).to.equal('idle');
      });
    });
  });

  it('should reconnect the catalog stream when the footer emits reconnect', () => {
    const firstSource = new MockEventSource();
    const secondSource = new MockEventSource();
    let callCount = 0;
    const observeCatalog = cy.stub().callsFake(() => {
      callCount++;
      return callCount === 1 ? firstSource : secondSource;
    }) as unknown as AssetService['observeCatalog'];

    mountWithCatalog(observeCatalog);
    cy.wrap(observeCatalog).should('have.been.calledOnce');
    cy.get('button[aria-label="Reconnect to catalog stream"]').click();
    cy.wrap(observeCatalog).should('have.been.calledTwice');
  });

  it('should replay pending mutations on init when the browser is online and mutations are queued', () => {
    const source = new MockEventSource();
    const replayQueue = cy.stub().resolves(undefined);
    mountWithCatalog(cy.stub().returns(source) as unknown as AssetService['observeCatalog'], {
      getPendingCount: cy.stub().resolves(3),
      replayQueue,
    });

    cy.contains('Syncing 3 pending changes').should('exist');
    cy.wrap(replayQueue).should('have.been.called');
  });

  it('should not show a syncing message when there are no pending mutations', () => {
    const source = new MockEventSource();
    mountWithCatalog(cy.stub().returns(source) as unknown as AssetService['observeCatalog'], { getPendingCount: cy.stub().resolves(0) });

    cy.contains('Syncing').should('not.exist');
  });

  it('should disconnect the catalog stream when the component is destroyed', () => {
    const source = new MockEventSource();
    const observeCatalogStub = cy.stub().returns(source) as unknown as AssetService['observeCatalog'];
    mountWithCatalog(observeCatalogStub).then(({ fixture }) => {
      waitForCatalogConnected(observeCatalogStub);
      cy.then(() => {
        cy.spy(source, 'close').as('closeSpy');
        fixture.destroy();
      });
      cy.get('@closeSpy').should('have.been.called');
    });
  });
});
