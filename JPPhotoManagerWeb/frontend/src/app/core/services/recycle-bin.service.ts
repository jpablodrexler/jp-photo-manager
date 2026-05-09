import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asset } from '../models/asset.model';
import { PaginatedData } from '../models/paginated-data.model';

@Injectable({ providedIn: 'root' })
export class RecycleBinService {

  private readonly baseUrl = '/api/recycle-bin';

  constructor(private http: HttpClient) {}

  getRecycleBin(page = 0): Observable<PaginatedData<Asset>> {
    return this.http.get<PaginatedData<Asset>>(this.baseUrl, { params: { page } });
  }

  restoreAssets(assetIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/restore`, { assetIds });
  }

  purgeAssets(assetIds: number[]): Observable<void> {
    return this.http.delete<void>(this.baseUrl, { body: { assetIds } });
  }

  purgeAll(): Observable<void> {
    return this.http.delete<void>(this.baseUrl);
  }
}
