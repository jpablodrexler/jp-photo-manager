import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

const STORAGE_KEY = 'photomanager_theme';
const ACCENT_STORAGE_KEY = 'photomanager_accent_color';

describe('ThemeService', () => {
  function createService(): ThemeService {
    TestBed.configureTestingModule({ providers: [ThemeService] });
    return TestBed.inject(ThemeService);
  }

  afterEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(ACCENT_STORAGE_KEY);
    document.documentElement.classList.remove('theme-dark', 'theme-light');
    document.documentElement.style.removeProperty('--accent-color');
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

  it('setAccentColor_validHex_setsCSSVariable', () => {
    const service = createService();
    service.setAccentColor('#1565c0');
    expect(document.documentElement.style.getPropertyValue('--accent-color')).to.equal('#1565c0');
  });

  it('setAccentColor_validHex_updatesMetaThemeColor', () => {
    const meta = document.createElement('meta');
    meta.name = 'theme-color';
    meta.setAttribute('content', '#2e7d32');
    document.head.appendChild(meta);

    const service = createService();
    service.setAccentColor('#1565c0');
    expect(document.querySelector('meta[name="theme-color"]')?.getAttribute('content')).to.equal('#1565c0');

    document.head.removeChild(meta);
  });

  it('setAccentColor_validHex_persistsToLocalStorage', () => {
    const service = createService();
    service.setAccentColor('#6a1b9a');
    expect(localStorage.getItem(ACCENT_STORAGE_KEY)).to.equal('#6a1b9a');
  });

  it('setAccentColor_validHex_emitsOnAccentColor$', () => {
    const service = createService();
    const emitted: string[] = [];
    service.accentColor$.subscribe(c => emitted.push(c));
    service.setAccentColor('#00695c');
    expect(emitted).to.include('#00695c');
  });

  it('init_storedAccentColor_appliesSavedColor', () => {
    localStorage.setItem(ACCENT_STORAGE_KEY, '#bf360c');
    const service = createService();
    service.init();
    expect(document.documentElement.style.getPropertyValue('--accent-color')).to.equal('#bf360c');
  });

  it('init_noStoredAccentColor_usesDefault', () => {
    localStorage.removeItem(ACCENT_STORAGE_KEY);
    const service = createService();
    service.init();
    expect(document.documentElement.style.getPropertyValue('--accent-color')).to.equal('');
  });
});
