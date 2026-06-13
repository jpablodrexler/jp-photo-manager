import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';
import { AnalyticsData } from '../models/analytics.model';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  const mockData: AnalyticsData = {
    folderStorage: [{ folderPath: '/photos', bytes: 1000000 }],
    formatDistribution: [{ extension: 'jpg', count: 100 }],
    photosPerMonth: [{ month: '2024-01', count: 10 }],
    ratingDistribution: [{ rating: 0, count: 50 }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AnalyticsService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAnalytics_onSuccess_returnsAnalyticsData', () => {
    service.getAnalytics().subscribe(data => {
      expect(data).to.deep.equal(mockData);
    });

    const req = httpMock.expectOne('/api/analytics');
    expect(req.request.method).to.equal('GET');
    req.flush(mockData);
  });

  it('getAnalytics_onError_propagatesError', () => {
    let errorReceived = false;

    service.getAnalytics().subscribe({
      next: () => { throw new Error('should not emit'); },
      error: () => { errorReceived = true; },
    });

    const req = httpMock.expectOne('/api/analytics');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

    expect(errorReceived).to.be.true;
  });
});
