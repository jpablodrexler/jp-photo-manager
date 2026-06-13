import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { TreeMapModule, PieChartModule, BarChartModule } from '@swimlane/ngx-charts';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsData, ChartEntry } from '../../core/models/analytics.model';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    TreeMapModule,
    PieChartModule,
    BarChartModule,
  ],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  data: AnalyticsData | null = null;
  loading = true;
  error = false;

  folderStorageSeries: ChartEntry[] = [];
  formatSeries: ChartEntry[] = [];
  photosPerMonthSeries: ChartEntry[] = [];
  ratingSeriesBarData: ChartEntry[] = [];

  constructor(private readonly analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.analyticsService.getAnalytics().subscribe({
      next: (d) => {
        this.data = d;
        this.folderStorageSeries = d.folderStorage.map(e => ({
          name: e.folderPath,
          value: e.bytes,
        }));
        this.formatSeries = d.formatDistribution.map(e => ({
          name: e.extension,
          value: e.count,
        }));
        this.photosPerMonthSeries = d.photosPerMonth.map(e => ({ name: e.month, value: e.count }));
        this.ratingSeriesBarData = d.ratingDistribution.map(e => ({
          name: String(e.rating),
          value: e.count,
        }));
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      },
    });
  }
}
