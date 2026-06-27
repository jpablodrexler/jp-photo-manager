import { Component, OnDestroy, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AssetService } from '../../../core/services/asset.service';
import { CatalogNotification } from '../../../core/models/catalog-notification.model';

type CatalogState = 'idle' | 'running';

@Component({
  selector: 'app-catalog-progress-footer',
  standalone: true,
  imports: [DatePipe, MatIconModule, MatButtonModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './catalog-progress-footer.component.html',
  styleUrl: './catalog-progress-footer.component.scss',
})
export class CatalogProgressFooterComponent implements OnInit, OnDestroy {
  state: CatalogState = 'idle';
  percentCompleted = 0;
  currentStatusText = '';
  lastCompletedAt: Date | null = null;

  private eventSource?: EventSource;

  constructor(private readonly assetService: AssetService) {}

  ngOnInit(): void {
    this.connect();
  }

  ngOnDestroy(): void {
    this.closeEventSource();
  }

  connect(): void {
    this.closeEventSource();
    this.state = 'idle';
    this.percentCompleted = 0;
    this.currentStatusText = '';

    this.eventSource = this.assetService.observeCatalog();

    this.eventSource.addEventListener('catalog', (event: MessageEvent) => {
      this.state = 'running';
      const notification = JSON.parse(event.data as string) as CatalogNotification;
      this.percentCompleted = notification.percentCompleted;
      if (notification.folderPath) {
        this.currentStatusText = notification.folderPath;
      } else if (notification.asset?.fileName) {
        this.currentStatusText = notification.asset.fileName;
      }
    });

    this.eventSource.addEventListener('catalog-done', () => {
      this.state = 'idle';
      this.lastCompletedAt = new Date();
    });

    this.eventSource.onerror = () => {
      if (this.state === 'running') {
        this.state = 'idle';
      }
    };
  }

  private closeEventSource(): void {
    this.eventSource?.close();
    this.eventSource = undefined;
  }
}
