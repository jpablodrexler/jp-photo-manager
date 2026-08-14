import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AlbumService } from './album.service';
import { Album, AlbumSummary, CreateAlbumRequest, UpdateAlbumRequest } from '../models/album.model';
import { Asset } from '../models/asset.model';

describe('AlbumService', () => {
  let service: AlbumService;
  let httpMock: HttpTestingController;

  const mockSummary: AlbumSummary = {
    albumId: 1,
    name: 'Vacation',
    description: 'Summer trip',
    assetCount: 3,
    createdAt: '2024-01-01T00:00:00',
  };

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

  const mockAlbum: Album = {
    albumId: 1,
    name: 'Vacation',
    description: 'Summer trip',
    createdAt: '2024-01-01T00:00:00',
    assets: { items: [mockAsset], pageIndex: 0, totalPages: 1, totalItems: 1 },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AlbumService,
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AlbumService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/albums and return the album summaries', () => {
    service.getAlbums().subscribe(albums => {
      expect(albums).to.deep.equal([mockSummary]);
    });

    const req = httpMock.expectOne('/api/albums');
    expect(req.request.method).to.equal('GET');
    req.flush([mockSummary]);
  });

  it('should POST /api/albums with the create request body', () => {
    const createReq: CreateAlbumRequest = { name: 'Vacation', description: 'Summer trip' };

    service.createAlbum(createReq).subscribe(album => {
      expect(album).to.deep.equal(mockSummary);
    });

    const req = httpMock.expectOne('/api/albums');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal(createReq);
    req.flush(mockSummary);
  });

  it('should GET /api/albums/:id with a default page of 0', () => {
    service.getAlbum(1).subscribe(album => {
      expect(album).to.deep.equal(mockAlbum);
    });

    const req = httpMock.expectOne(r => r.url === '/api/albums/1');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('page')).to.equal('0');
    req.flush(mockAlbum);
  });

  it('should GET /api/albums/:id with the given page', () => {
    service.getAlbum(1, 2).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/albums/1');
    expect(req.request.params.get('page')).to.equal('2');
    req.flush(mockAlbum);
  });

  it('should PUT /api/albums/:id with the update request body', () => {
    const updateReq: UpdateAlbumRequest = { name: 'Renamed', description: 'Updated' };

    service.updateAlbum(1, updateReq).subscribe(album => {
      expect(album).to.deep.equal(mockSummary);
    });

    const req = httpMock.expectOne('/api/albums/1');
    expect(req.request.method).to.equal('PUT');
    expect(req.request.body).to.deep.equal(updateReq);
    req.flush(mockSummary);
  });

  it('should DELETE /api/albums/:id', () => {
    service.deleteAlbum(1).subscribe();

    const req = httpMock.expectOne('/api/albums/1');
    expect(req.request.method).to.equal('DELETE');
    req.flush(null);
  });

  it('should POST /api/albums/:id/assets with the asset ids to add', () => {
    service.addAssets(1, [10, 20]).subscribe();

    const req = httpMock.expectOne('/api/albums/1/assets');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ assetIds: [10, 20] });
    req.flush(null);
  });

  it('should DELETE /api/albums/:id/assets with a body of asset ids to remove', () => {
    service.removeAssets(1, [10, 20]).subscribe();

    const req = httpMock.expectOne('/api/albums/1/assets');
    expect(req.request.method).to.equal('DELETE');
    expect(req.request.body).to.deep.equal({ assetIds: [10, 20] });
    req.flush(null);
  });
});
