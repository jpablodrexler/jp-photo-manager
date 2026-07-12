import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ConvertService } from './convert.service';
import { ConvertAssetsDirectoriesDefinition } from '../models/convert-config.model';

describe('ConvertService', () => {
  let service: ConvertService;
  let httpMock: HttpTestingController;

  const mockDefinitions: ConvertAssetsDirectoriesDefinition[] = [
    {
      sourceDirectory: '/photos/png',
      destinationDirectory: '/photos/jpeg',
      includeSubFolders: true,
      deleteAssetsNotInSource: false,
      order: 0,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ConvertService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ConvertService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/convert/configuration', () => {
    service.getConfiguration().subscribe(defs => {
      expect(defs).to.deep.equal(mockDefinitions);
    });

    const req = httpMock.expectOne('/api/convert/configuration');
    expect(req.request.method).to.equal('GET');
    req.flush(mockDefinitions);
  });

  it('should PUT /api/convert/configuration with definitions body', () => {
    service.setConfiguration(mockDefinitions).subscribe();

    const req = httpMock.expectOne('/api/convert/configuration');
    expect(req.request.method).to.equal('PUT');
    expect(req.request.body).to.deep.equal(mockDefinitions);
    req.flush(null);
  });

  it('should return an EventSource for convert SSE', () => {
    const source = service.run();
    expect(source).to.be.instanceOf(EventSource);
    expect(source.url).to.contain('/api/convert/run');
    source.close();
  });
});
