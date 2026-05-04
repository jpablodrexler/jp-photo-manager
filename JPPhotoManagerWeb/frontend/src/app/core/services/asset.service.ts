import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asset, SortCriteria } from '../models/asset.model';
import { ExifMetadata } from '../models/exif-metadata.model';
import { PaginatedData } from '../models/paginated-data.model';

@Injectable({ providedIn: 'root' })
export class AssetService {

  private readonly baseUrl = '/api/assets';

  constructor(private http: HttpClient) {}

  getAssets(folderPath: string, page = 0, sort: SortCriteria = 'FILE_NAME'): Observable<PaginatedData<Asset>> {
    const params = new HttpParams()
      .set('folderPath', folderPath)
      .set('page', page)
      .set('sort', sort);
    return this.http.get<PaginatedData<Asset>>(this.baseUrl, { params });
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
}
