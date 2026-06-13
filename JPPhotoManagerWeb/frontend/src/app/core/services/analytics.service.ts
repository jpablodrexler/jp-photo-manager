import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyticsData } from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly baseUrl = '/api/analytics';

  constructor(private readonly http: HttpClient) {}

  getAnalytics(): Observable<AnalyticsData> {
    return this.http.get<AnalyticsData>(this.baseUrl);
  }
}
