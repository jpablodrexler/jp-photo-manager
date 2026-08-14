import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { UserAdminService } from './user-admin.service';
import { UserAdmin } from '../models/user-admin.model';

describe('UserAdminService', () => {
  let service: UserAdminService;
  let httpMock: HttpTestingController;

  const mockUser: UserAdmin = { id: 'u1', username: 'alice', createdAt: '2024-01-01T00:00:00Z' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserAdminService, provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET /api/admin/users to list users', () => {
    service.getUsers().subscribe(users => {
      expect(users).to.deep.equal([mockUser]);
    });
    const req = httpMock.expectOne('/api/admin/users');
    expect(req.request.method).to.equal('GET');
    req.flush([mockUser]);
  });

  it('should POST /api/admin/users to create a user', () => {
    service.createUser('alice', 'hunter2').subscribe(user => {
      expect(user).to.deep.equal(mockUser);
    });
    const req = httpMock.expectOne('/api/admin/users');
    expect(req.request.method).to.equal('POST');
    expect(req.request.body).to.deep.equal({ username: 'alice', password: 'hunter2' });
    req.flush(mockUser);
  });

  it('should PATCH /api/admin/users/:id/password to update a password', () => {
    service.updatePassword('u1', 'newpass').subscribe();
    const req = httpMock.expectOne('/api/admin/users/u1/password');
    expect(req.request.method).to.equal('PATCH');
    expect(req.request.body).to.deep.equal({ password: 'newpass' });
    req.flush(null);
  });

  it('should DELETE /api/admin/users/:id to delete a user', () => {
    service.deleteUser('u1').subscribe();
    const req = httpMock.expectOne('/api/admin/users/u1');
    expect(req.request.method).to.equal('DELETE');
    req.flush(null);
  });
});
