import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RecycleBinService } from './recycle-bin.service';
import { Asset } from '../models/asset.model';
import { PaginatedData } from '../models/paginated-data.model';

describe('RecycleBinService', () => {
  let service: RecycleBinService;
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
    fileType: 'IMAGE',
    isVideo: false,
  };

  const mockPage: PaginatedData<Asset> = { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RecycleBinService,
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RecycleBinService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/recycle-bin with a default page of 0', () => {
    service.getRecycleBin().subscribe(page => {
      expect(page).to.deep.equal(mockPage);
    });

    const req = httpMock.expectOne(r => r.url === '/api/recycle-bin');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('page')).to.equal('0');
    req.flush(mockPage);
  });

  it('should GET /api/recycle-bin with the given page', () => {
    service.getRecycleBin(3).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/recycle-bin');
    expect(req.request.params.get('page')).to.equal('3');
    req.flush(mockPage);
  });

  it('should POST /api/recycle-bin/restore with the asset ids to restore', () => {
    service.restoreAssets([1, 2]).subscribe();

    const req = httpMock.expectOne('/api/recycle-bin/restore');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ assetIds: [1, 2] });
    req.flush(null);
  });

  it('should DELETE /api/recycle-bin with a body of asset ids to purge', () => {
    service.purgeAssets([1, 2]).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/recycle-bin' && r.method === 'DELETE' && r.body !== null);
    expect(req.request.body).to.deep.equal({ assetIds: [1, 2] });
    req.flush(null);
  });

  it('should DELETE /api/recycle-bin with no body to purge everything', () => {
    service.purgeAll().subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/recycle-bin' && r.method === 'DELETE' && r.body === null);
    req.flush(null);
  });
});
