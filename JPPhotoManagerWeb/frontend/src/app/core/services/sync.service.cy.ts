import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SyncService } from './sync.service';
import { SyncAssetsDirectoriesDefinition } from '../models/sync-config.model';

describe('SyncService', () => {
  let service: SyncService;
  let httpMock: HttpTestingController;

  const mockDefinitions: SyncAssetsDirectoriesDefinition[] = [
    {
      sourceDirectory: '/photos/source',
      destinationDirectory: '/photos/dest',
      includeSubFolders: true,
      deleteAssetsNotInSource: false,
      order: 0,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SyncService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(SyncService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/sync/configuration', () => {
    service.getConfiguration().subscribe(defs => {
      expect(defs).to.deep.equal(mockDefinitions);
    });

    const req = httpMock.expectOne('/api/sync/configuration');
    expect(req.request.method).to.equal('GET');
    req.flush(mockDefinitions);
  });

  it('should PUT /api/sync/configuration with definitions body', () => {
    service.setConfiguration(mockDefinitions).subscribe();

    const req = httpMock.expectOne('/api/sync/configuration');
    expect(req.request.method).to.equal('PUT');
    expect(req.request.body).to.deep.equal(mockDefinitions);
    req.flush(null);
  });

  it('should return an EventSource for sync SSE', () => {
    const source = service.run();
    expect(source).to.be.instanceOf(EventSource);
    source.close();
  });
});
