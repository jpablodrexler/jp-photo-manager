import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TagService } from './tag.service';

describe('TagService', () => {
  let service: TagService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TagService, provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    service = TestBed.inject(TagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should POST /api/assets/:id/tags to add a tag', () => {
    service.addTag(1, 'vacation').subscribe();
    const req = httpMock.expectOne('/api/assets/1/tags');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ name: 'vacation' });
    req.flush(null);
  });

  it('should DELETE /api/assets/:id/tags with the tag name as a query param', () => {
    service.removeTag(1, 'vacation').subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/assets/1/tags');
    expect(req.request.method).to.equal('DELETE');
    expect(req.request.params.get('name')).to.equal('vacation');
    req.flush(null);
  });

  it('should GET /api/tags with the search query param', () => {
    service.searchTags('va').subscribe(tags => {
      expect(tags).to.deep.equal(['vacation', 'valley']);
    });
    const req = httpMock.expectOne(r => r.url === '/api/tags');
    expect(req.request.method).to.equal('GET');
    expect(req.request.params.get('q')).to.equal('va');
    req.flush(['vacation', 'valley']);
  });

  it('should POST /api/assets/tags/bulk to bulk-add a tag', () => {
    service.bulkAddTag([1, 2, 3], 'beach').subscribe();
    const req = httpMock.expectOne('/api/assets/tags/bulk');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ assetIds: [1, 2, 3], name: 'beach' });
    req.flush(null);
  });

  it('should DELETE /api/assets/tags/bulk with a body to bulk-remove a tag', () => {
    service.bulkRemoveTag([1, 2, 3], 'beach').subscribe();
    const req = httpMock.expectOne('/api/assets/tags/bulk');
    expect(req.request.method).to.equal('DELETE');
    expect(req.request.body).to.deep.equal({ assetIds: [1, 2, 3], name: 'beach' });
    req.flush(null);
  });
});
