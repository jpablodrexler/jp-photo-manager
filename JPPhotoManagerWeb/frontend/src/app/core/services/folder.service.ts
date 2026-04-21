import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Folder } from '../models/folder.model';

@Injectable({ providedIn: 'root' })
export class FolderService {

  private readonly baseUrl = '/api/folders';

  constructor(private http: HttpClient) {}

  getFolders(parentPath?: string): Observable<Folder[]> {
    let params = new HttpParams();
    if (parentPath) {
      params = params.set('parentPath', parentPath);
    }
    return this.http.get<Folder[]>(this.baseUrl, { params });
  }

  getDrives(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/drives`);
  }

  getInitialFolder(): Observable<string> {
    return this.http.get(`${this.baseUrl}/initial`, { responseType: 'text' });
  }

  getRecentTargetPaths(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/recent-paths`);
  }
}
