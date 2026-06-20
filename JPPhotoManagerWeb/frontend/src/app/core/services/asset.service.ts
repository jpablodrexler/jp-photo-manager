import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Asset, CropAssetRequest, RenameAssetsResponse, SortCriteria } from '../models/asset.model';
import { ExifMetadata } from '../models/exif-metadata.model';
import { PaginatedData } from '../models/paginated-data.model';
import { TimelineGroup } from '../models/timeline-group.model';
import { BackgroundSyncService } from './background-sync.service';

@Injectable({ providedIn: 'root' })
export class AssetService {

  private readonly baseUrl = '/api/assets';

  constructor(
    private http: HttpClient,
    private backgroundSyncService: BackgroundSyncService
  ) {}

  getAssets(folderPath: string, page = 0, sort: SortCriteria = 'FILE_NAME',
            search?: string, dateFrom?: string, dateTo?: string, minRating?: number,
            tags?: string[]): Observable<PaginatedData<Asset>> {
    let params = new HttpParams()
      .set('folderPath', folderPath)
      .set('page', page)
      .set('sort', sort);
    if (search) params = params.set('search', search);
    if (dateFrom) params = params.set('dateFrom', dateFrom);
    if (dateTo)   params = params.set('dateTo', dateTo);
    if (minRating && minRating > 0) params = params.set('minRating', minRating);
    if (tags && tags.length > 0) params = params.set('tags', tags.join(','));
    return this.http.get<PaginatedData<Asset>>(this.baseUrl, { params });
  }

  getTimeline(folderPath: string, page = 0,
              filters?: { search?: string; dateFrom?: string; dateTo?: string; minRating?: number }
  ): Observable<PaginatedData<TimelineGroup>> {
    let params = new HttpParams()
      .set('folderPath', folderPath)
      .set('page', page);
    if (filters?.search) params = params.set('search', filters.search);
    if (filters?.dateFrom) params = params.set('dateFrom', filters.dateFrom);
    if (filters?.dateTo) params = params.set('dateTo', filters.dateTo);
    if (filters?.minRating && filters.minRating > 0) params = params.set('minRating', filters.minRating);
    return this.http.get<PaginatedData<TimelineGroup>>(`${this.baseUrl}/timeline`, { params });
  }

  rateAsset(assetId: number, rating: number): Observable<void> {
    const url = `${this.baseUrl}/${assetId}/rating`;
    const body = { rating };
    return this.http.patch<void>(url, body).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 0) {
          this.backgroundSyncService.queueMutation(url, 'PATCH', body);
          return throwError(() => error);
        }
        return throwError(() => error);
      })
    );
  }

  getThumbnailUrl(assetId: number): string {
    return `${this.baseUrl}/${assetId}/thumbnail`;
  }

  moveAssets(assetIds: number[], destinationFolderPath: string, preserveOriginal: boolean): Observable<boolean> {
    return this.http.post<boolean>(`${this.baseUrl}/move`, {
      assetIds,
      destinationFolderPath,
      preserveOriginal
    });
  }

  deleteAssets(assetIds: number[], deleteFiles = false): Observable<void> {
    const params = new HttpParams()
      .set('assetIds', assetIds.join(','))
      .set('deleteFiles', deleteFiles);
    return this.http.delete<void>(this.baseUrl, { params });
  }

  getDuplicatedAssets(): Observable<Asset[][]> {
    return this.http.get<Asset[][]>(`${this.baseUrl}/duplicates`);
  }

  catalogAssets(): EventSource {
    return new EventSource(`${this.baseUrl}/catalog`);
  }

  getExifMetadata(assetId: number): Observable<ExifMetadata | null> {
    return this.http.get<ExifMetadata | null>(`${this.baseUrl}/${assetId}/exif`);
  }

  downloadAssets(assetIds: number[]): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/download`, { assetIds }, { responseType: 'blob' as 'json' }) as Observable<Blob>;
  }

  uploadAsset(folderPath: string, file: File): Observable<HttpEvent<Asset>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folderPath', folderPath);
    return this.http.post<Asset>(`${this.baseUrl}/upload`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  renameAssets(assetIds: number[], pattern: string, applied: boolean): Observable<RenameAssetsResponse> {
    return this.http.post<RenameAssetsResponse>(`${this.baseUrl}/rename`, { assetIds, pattern, applied });
  }

  cropAsset(assetId: number, request: CropAssetRequest): Observable<Asset> {
    return this.http.post<Asset>(`${this.baseUrl}/${assetId}/crop`, request);
  }

  getPlaylist(assetId: number): Observable<Asset[]> {
    return this.http.get<Asset[]>(`/api/audio/playlist/${assetId}`);
  }
}
