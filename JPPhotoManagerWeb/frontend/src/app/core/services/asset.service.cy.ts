import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AssetService } from './asset.service';
import { ExifMetadata } from '../models/exif-metadata.model';
import { PaginatedData } from '../models/paginated-data.model';
import { Asset } from '../models/asset.model';

describe('AssetService', () => {
  let service: AssetService;
  let httpMock: HttpTestingController;

  const mockAsset: Asset = {
    assetId: 1,
    folderId: 1,
    folderPath: '/photos',
    fileName: 'photo.jpg',
    fileSize: 102400,
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
    hash: 'abc123',
    thumbnailUrl: '/api/assets/1/thumbnail',
    imageUrl: '/api/assets/1/image',
    rating: 0,
    tags: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AssetService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AssetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/assets with correct query params', () => {
    const mockPage: PaginatedData<Asset> = { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 };

    service.getAssets('/photos', 0, 'FILE_NAME').subscribe(data => {
      expect(data).to.deep.equal(mockPage);
    });

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('folderPath')).to.equal('/photos');
    expect(req.request.params.get('page')).to.equal('0');
    expect(req.request.params.get('sort')).to.equal('FILE_NAME');
    req.flush(mockPage);
  });

  it('should use default page 0 and sort FILE_NAME when omitted', () => {
    service.getAssets('/photos').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.params.get('page')).to.equal('0');
    expect(req.request.params.get('sort')).to.equal('FILE_NAME');
    req.flush({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 });
  });

  it('should return a thumbnail URL string', () => {
    expect(service.getThumbnailUrl(42)).to.equal('/api/assets/42/thumbnail');
  });

  it('should POST /api/assets/move with correct body', () => {
    service.moveAssets([1, 2], '/dest', true).subscribe(result => {
      expect(result).to.be.true;
    });

    const req = httpMock.expectOne('/api/assets/move');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ assetIds: [1, 2], destinationFolderPath: '/dest', preserveOriginal: true });
    req.flush(true);
  });

  it('should DELETE /api/assets with assetIds and deleteFiles params', () => {
    service.deleteAssets([1, 2], true).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.method).to.equal('DELETE');
    expect(req.request.params.get('assetIds')).to.equal('1,2');
    expect(req.request.params.get('deleteFiles')).to.equal('true');
    req.flush(null);
  });

  it('should default deleteFiles to false when omitted', () => {
    service.deleteAssets([3]).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets');
    expect(req.request.params.get('deleteFiles')).to.equal('false');
    req.flush(null);
  });

  it('should GET /api/assets/duplicates', () => {
    const mockGroups: Asset[][] = [[mockAsset]];

    service.getDuplicatedAssets().subscribe(groups => {
      expect(groups).to.deep.equal(mockGroups);
    });

    const req = httpMock.expectOne('/api/assets/duplicates');
    expect(req.request.method).to.equal('GET');
    req.flush(mockGroups);
  });

  it('should return an EventSource for catalog SSE', () => {
    const source = service.catalogAssets();
    expect(source).to.be.instanceOf(EventSource);
    source.close();
  });

  it('getExifMetadata_validId_issuesGetRequestAndReturnsMappedObject', () => {
    const mockExif: ExifMetadata = {
      cameraMake: 'Canon',
      cameraModel: 'EOS 90D',
      lensModel: null,
      exposureTime: '1/500',
      fNumber: 5.6,
      isoSpeed: 200,
      focalLength: 85,
      dateTaken: '2024-06-15T10:30:00',
      widthPixels: 6000,
      heightPixels: 4000,
      gpsLatitude: null,
      gpsLongitude: null,
    };

    service.getExifMetadata(1).subscribe(data => {
      expect(data).to.deep.equal(mockExif);
    });

    const req = httpMock.expectOne('/api/assets/1/exif');
    expect(req.request.method).to.equal('GET');
    req.flush(mockExif);
  });

  it('getExifMetadata_assetWithNoExif_returnsNull', () => {
    service.getExifMetadata(99).subscribe(data => {
      expect(data).to.be.null;
    });

    const req = httpMock.expectOne('/api/assets/99/exif');
    req.flush(null);
  });

  it('getTimeline_withFolderAndPage_sendsCorrectParams', () => {
    const mockPage = { items: [], pageIndex: 0, totalPages: 0, totalItems: 0 };

    service.getTimeline('/photos', 2).subscribe(data => {
      expect(data).to.deep.equal(mockPage);
    });

    const req = httpMock.expectOne(r => r.url === '/api/assets/timeline');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('folderPath')).to.equal('/photos');
    expect(req.request.params.get('page')).to.equal('2');
    req.flush(mockPage);
  });

  it('getTimeline_withFilters_forwardsAllFilterParams', () => {
    service.getTimeline('/photos', 0, {
      search: 'beach',
      dateFrom: '2024-01-01',
      dateTo: '2024-12-31',
      minRating: 3,
    }).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets/timeline');
    expect(req.request.params.get('search')).to.equal('beach');
    expect(req.request.params.get('dateFrom')).to.equal('2024-01-01');
    expect(req.request.params.get('dateTo')).to.equal('2024-12-31');
    expect(req.request.params.get('minRating')).to.equal('3');
    req.flush({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 });
  });

  it('getTimeline_withNoFilters_omitsOptionalParams', () => {
    service.getTimeline('/photos').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/assets/timeline');
    expect(req.request.params.has('search')).to.be.false;
    expect(req.request.params.has('dateFrom')).to.be.false;
    expect(req.request.params.has('dateTo')).to.be.false;
    expect(req.request.params.has('minRating')).to.be.false;
    req.flush({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 });
  });
});
