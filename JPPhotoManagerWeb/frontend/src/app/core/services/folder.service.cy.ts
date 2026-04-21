import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FolderService } from './folder.service';
import { Folder } from '../models/folder.model';

describe('FolderService', () => {
  let service: FolderService;
  let httpMock: HttpTestingController;

  const mockFolders: Folder[] = [
    { folderId: 1, path: '/photos', name: 'photos' },
    { folderId: 2, path: '/photos/vacations', name: 'vacations', parentPath: '/photos' },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FolderService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(FolderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/folders without parentPath when omitted', () => {
    service.getFolders().subscribe(folders => {
      expect(folders).to.deep.equal(mockFolders);
    });

    const req = httpMock.expectOne(r => r.url === '/api/folders' && !r.params.has('parentPath'));
    expect(req.request.method).to.equal('GET');
    req.flush(mockFolders);
  });

  it('should GET /api/folders with parentPath when provided', () => {
    service.getFolders('/photos').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/folders' && r.params.has('parentPath'));
    expect(req.request.params.get('parentPath')).to.equal('/photos');
    req.flush([]);
  });

  it('should GET /api/folders/drives', () => {
    const mockDrives = ['C:', 'D:'];

    service.getDrives().subscribe(drives => {
      expect(drives).to.deep.equal(mockDrives);
    });

    const req = httpMock.expectOne('/api/folders/drives');
    expect(req.request.method).to.equal('GET');
    req.flush(mockDrives);
  });

  it('should GET /api/folders/initial', () => {
    service.getInitialFolder().subscribe(folder => {
      expect(folder).to.equal('/home/user/Pictures');
    });

    const req = httpMock.expectOne('/api/folders/initial');
    expect(req.request.method).to.equal('GET');
    req.flush('/home/user/Pictures');
  });

  it('should GET /api/folders/recent-paths', () => {
    const mockPaths = ['/photos/events', '/photos/vacations'];

    service.getRecentTargetPaths().subscribe(paths => {
      expect(paths).to.deep.equal(mockPaths);
    });

    const req = httpMock.expectOne('/api/folders/recent-paths');
    expect(req.request.method).to.equal('GET');
    req.flush(mockPaths);
  });
});
