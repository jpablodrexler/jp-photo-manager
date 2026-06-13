import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

const STORAGE_KEY = 'photomanager_theme';

describe('ThemeService', () => {
  function createService(): ThemeService {
    TestBed.configureTestingModule({ providers: [ThemeService] });
    return TestBed.inject(ThemeService);
  }

  afterEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    document.documentElement.classList.remove('theme-dark', 'theme-light');
    TestBed.resetTestingModule();
  });

  it('init_storedDarkPreference_appliesDarkClass', () => {
    localStorage.setItem(STORAGE_KEY, 'dark');
    const service = createService();
    service.init();
    expect(document.documentElement.classList.contains('theme-dark')).to.be.true;
    expect(document.documentElement.classList.contains('theme-light')).to.be.false;
  });

  it('init_storedLightPreference_appliesLightClass', () => {
    localStorage.setItem(STORAGE_KEY, 'light');
    const service = createService();
    service.init();
    expect(document.documentElement.classList.contains('theme-light')).to.be.true;
    expect(document.documentElement.classList.contains('theme-dark')).to.be.false;
  });

  it('init_noStoredPreference_systemDark_appliesDarkClass', () => {
    localStorage.removeItem(STORAGE_KEY);
    cy.stub(window, 'matchMedia').returns({
      matches: true,
      media: '',
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => true,
    } as MediaQueryList);
    const service = createService();
    service.init();
    expect(document.documentElement.classList.contains('theme-dark')).to.be.true;
  });

  it('toggle_currentlyDark_switchesToLightAndWritesLocalStorage', () => {
    localStorage.setItem(STORAGE_KEY, 'dark');
    const service = createService();
    service.init();
    const newMode = service.toggle();
    expect(newMode).to.equal('light');
    expect(localStorage.getItem(STORAGE_KEY)).to.equal('light');
    expect(document.documentElement.classList.contains('theme-light')).to.be.true;
    expect(document.documentElement.classList.contains('theme-dark')).to.be.false;
  });

  it('toggle_currentlyLight_switchesToDark', () => {
    localStorage.setItem(STORAGE_KEY, 'light');
    const service = createService();
    service.init();
    const newMode = service.toggle();
    expect(newMode).to.equal('dark');
    expect(localStorage.getItem(STORAGE_KEY)).to.equal('dark');
    expect(document.documentElement.classList.contains('theme-dark')).to.be.true;
  });
});
