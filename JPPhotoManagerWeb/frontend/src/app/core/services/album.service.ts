import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Album, AlbumSummary, CreateAlbumRequest, UpdateAlbumRequest } from '../models/album.model';

@Injectable({ providedIn: 'root' })
export class AlbumService {

  private readonly baseUrl = '/api/albums';

  constructor(private http: HttpClient) {}

  getAlbums(): Observable<AlbumSummary[]> {
    return this.http.get<AlbumSummary[]>(this.baseUrl);
  }

  createAlbum(req: CreateAlbumRequest): Observable<AlbumSummary> {
    return this.http.post<AlbumSummary>(this.baseUrl, req);
  }

  getAlbum(id: number, page = 0): Observable<Album> {
    return this.http.get<Album>(`${this.baseUrl}/${id}`, { params: { page } });
  }

  updateAlbum(id: number, req: UpdateAlbumRequest): Observable<AlbumSummary> {
    return this.http.put<AlbumSummary>(`${this.baseUrl}/${id}`, req);
  }

  deleteAlbum(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addAssets(albumId: number, assetIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${albumId}/assets`, { assetIds });
  }

  removeAssets(albumId: number, assetIds: number[]): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${albumId}/assets`, { body: { assetIds } });
  }
}
