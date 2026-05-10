import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserAdmin } from '../models/user-admin.model';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserAdmin[]> {
    return this.http.get<UserAdmin[]>('/api/admin/users');
  }

  createUser(username: string, password: string): Observable<UserAdmin> {
    return this.http.post<UserAdmin>('/api/admin/users', { username, password });
  }

  updatePassword(id: string, password: string): Observable<void> {
    return this.http.patch<void>(`/api/admin/users/${id}/password`, { password });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/users/${id}`);
  }
}
