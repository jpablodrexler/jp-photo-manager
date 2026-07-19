import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TagService {

  private readonly baseUrl = '/api/assets';
  private readonly tagsUrl = '/api/tags';

  constructor(private http: HttpClient) {}

  addTag(assetId: number, name: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${assetId}/tags`, { name });
  }

  removeTag(assetId: number, name: string): Observable<void> {
    const params = new HttpParams().set('name', name);
    return this.http.delete<void>(`${this.baseUrl}/${assetId}/tags`, { params });
  }

  searchTags(q: string): Observable<string[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<string[]>(this.tagsUrl, { params });
  }

  bulkAddTag(assetIds: number[], name: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/tags/bulk`, { assetIds, name });
  }

  bulkRemoveTag(assetIds: number[], name: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tags/bulk`, { body: { assetIds, name } });
  }
}
