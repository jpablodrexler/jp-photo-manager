import { Component, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
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
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    TreeMapModule,
    PieChartModule,
    BarChartModule,
  ],
  templateUrl: './analytics.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  readonly data = signal<AnalyticsData | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  readonly folderStorageSeries = signal<ChartEntry[]>([]);
  readonly formatSeries = signal<ChartEntry[]>([]);
  readonly photosPerMonthSeries = signal<ChartEntry[]>([]);
  readonly ratingSeriesBarData = signal<ChartEntry[]>([]);

  constructor(private readonly analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.analyticsService.getAnalytics().subscribe({
      next: (d) => {
        this.data.set(d);
        this.folderStorageSeries.set(d.folderStorage.map(e => ({
          name: e.folderPath,
          value: e.bytes,
        })));
        this.formatSeries.set(d.formatDistribution.map(e => ({
          name: e.extension,
          value: e.count,
        })));
        this.photosPerMonthSeries.set(d.photosPerMonth.map(e => ({ name: e.month, value: e.count })));
        this.ratingSeriesBarData.set(d.ratingDistribution.map(e => ({
          name: String(e.rating),
          value: e.count,
        })));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
