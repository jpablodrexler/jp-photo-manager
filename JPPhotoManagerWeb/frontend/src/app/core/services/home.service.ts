import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HomeStats } from '../models/home-stats.model';

@Injectable({ providedIn: 'root' })
export class HomeService {
  constructor(private http: HttpClient) {}

  getStats(): Observable<HomeStats> {
    return this.http.get<HomeStats>('/api/home/stats');
  }
}
