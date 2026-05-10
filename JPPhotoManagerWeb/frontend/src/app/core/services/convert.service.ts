import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConvertAssetsDirectoriesDefinition, ConvertAssetsResult } from '../models/convert-config.model';

@Injectable({ providedIn: 'root' })
export class ConvertService {

  private readonly baseUrl = '/api/convert';

  constructor(private http: HttpClient) {}

  getConfiguration(): Observable<ConvertAssetsDirectoriesDefinition[]> {
    return this.http.get<ConvertAssetsDirectoriesDefinition[]>(`${this.baseUrl}/configuration`);
  }

  setConfiguration(definitions: ConvertAssetsDirectoriesDefinition[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/configuration`, definitions);
  }

  run(): EventSource {
    return new EventSource(`${this.baseUrl}/run`);
  }
}
