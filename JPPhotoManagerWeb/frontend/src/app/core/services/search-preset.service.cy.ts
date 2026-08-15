import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SearchPresetService } from './search-preset.service';
import { SearchPreset } from '../models/search-preset.model';

describe('SearchPresetService', () => {
  let service: SearchPresetService;
  let httpMock: HttpTestingController;

  const mockPreset: SearchPreset = {
    presetId: 1,
    name: 'Birthday 2024',
    createdAt: '2024-06-01T00:00:00Z',
    search: 'birthday',
    dateFrom: '2024-01-01',
    dateTo: '2024-12-31',
    minRating: 4,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SearchPresetService, provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    service = TestBed.inject(SearchPresetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/search-presets to list presets', () => {
    service.listPresets().subscribe(presets => {
      expect(presets).to.deep.equal([mockPreset]);
    });
    const req = httpMock.expectOne('/api/search-presets');
    expect(req.request.method).to.equal('GET');
    req.flush([mockPreset]);
  });

  it('should POST /api/search-presets to create a preset', () => {
    const request = { name: 'Birthday 2024', search: 'birthday', minRating: 4 };
    service.createPreset(request).subscribe(preset => {
      expect(preset).to.deep.equal(mockPreset);
    });
    const req = httpMock.expectOne('/api/search-presets');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal(request);
    req.flush(mockPreset);
  });

  it('should DELETE /api/search-presets/:id to delete a preset', () => {
    service.deletePreset(1).subscribe();
    const req = httpMock.expectOne('/api/search-presets/1');
    expect(req.request.method).to.equal('DELETE');
    req.flush(null);
  });
});
