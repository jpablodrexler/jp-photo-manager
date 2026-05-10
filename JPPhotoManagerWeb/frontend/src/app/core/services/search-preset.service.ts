import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatePresetRequest, SearchPreset } from '../models/search-preset.model';

@Injectable({ providedIn: 'root' })
export class SearchPresetService {

  private readonly baseUrl = '/api/search-presets';

  constructor(private http: HttpClient) {}

  listPresets(): Observable<SearchPreset[]> {
    return this.http.get<SearchPreset[]>(this.baseUrl);
  }

  createPreset(req: CreatePresetRequest): Observable<SearchPreset> {
    return this.http.post<SearchPreset>(this.baseUrl, req);
  }

  deletePreset(presetId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${presetId}`);
  }
}
