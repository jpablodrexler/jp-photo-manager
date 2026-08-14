import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HomeService } from './home.service';
import { HomeStats } from '../models/home-stats.model';

describe('HomeService', () => {
  let service: HomeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [HomeService, provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    service = TestBed.inject(HomeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/home/stats', () => {
    const mockStats: HomeStats = {
      folderCount: 10,
      assetCount: 100,
      lastCatalogCompletedAt: '2024-06-01T00:00:00Z',
      totalFileSize: 1024000,
      duplicateCount: 3,
      topFolders: [{ path: '/photos', assetCount: 42 }],
      recentAssets: [
        { assetId: 1, fileName: 'a.jpg', folderPath: '/photos', thumbnailUrl: '/api/assets/1/thumbnail', fileSize: 1000 },
      ],
    };

    service.getStats().subscribe(data => {
      expect(data).to.deep.equal(mockStats);
    });

    const req = httpMock.expectOne('/api/home/stats');
    expect(req.request.method).to.equal('GET');
    req.flush(mockStats);
  });
});
