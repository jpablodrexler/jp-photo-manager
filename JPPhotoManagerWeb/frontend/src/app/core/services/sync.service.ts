import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SyncAssetsDirectoriesDefinition } from '../models/sync-config.model';

@Injectable({ providedIn: 'root' })
export class SyncService {

  private readonly baseUrl = '/api/sync';

  constructor(private http: HttpClient) {}

  getConfiguration(): Observable<SyncAssetsDirectoriesDefinition[]> {
    return this.http.get<SyncAssetsDirectoriesDefinition[]>(`${this.baseUrl}/configuration`);
  }

  setConfiguration(definitions: SyncAssetsDirectoriesDefinition[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/configuration`, definitions);
  }

  run(): EventSource {
    return new EventSource(`${this.baseUrl}/run`);
  }
}
