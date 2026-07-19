import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PreferenceService } from './preference.service';
import { ThemeService } from './theme.service';

describe('PreferenceService', () => {
  let service: PreferenceService;
  let httpMock: HttpTestingController;
  let applyThemeStub: ReturnType<typeof cy.stub>;

  beforeEach(() => {
    applyThemeStub = cy.stub();
    const themeServiceStub: Partial<ThemeService> = {
      applyTheme: applyThemeStub as unknown as ThemeService['applyTheme'],
    };
    TestBed.configureTestingModule({
      providers: [
        PreferenceService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ThemeService, useValue: themeServiceStub },
      ],
    });
    service = TestBed.inject(PreferenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('should call GET and apply the theme mode on success', () => {
    let completed = false;
    service.load().subscribe({ complete: () => { completed = true; } });

    const req = httpMock.expectOne('/api/preferences');
    expect(req.request.method).to.equal('GET');
    req.flush({ themeMode: 'light' });

    expect(completed).to.be.true;
    cy.wrap(applyThemeStub).should('have.been.calledWith', 'light');
  });

  it('should swallow the error silently on failure', () => {
    let completed = false;
    let errored = false;
    service.load().subscribe({
      complete: () => { completed = true; },
      error: () => { errored = true; },
    });

    const req = httpMock.expectOne('/api/preferences');
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });

    expect(completed).to.be.true;
    expect(errored).to.be.false;
  });

  it('should call PUT with the correct body when saving', () => {
    service.save('dark').subscribe();
    const req = httpMock.expectOne('/api/preferences');
    expect(req.request.method).to.equal('PUT');
    expect(req.request.body).to.deep.equal({ themeMode: 'dark' });
    req.flush(null);
  });
});
