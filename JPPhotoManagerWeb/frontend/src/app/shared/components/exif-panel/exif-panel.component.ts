import { Component, EventEmitter, HostBinding, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from '../../../core/services/asset.service';
import { ExifMetadata } from '../../../core/models/exif-metadata.model';

@Component({
  selector: 'app-exif-panel',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatListModule, MatProgressSpinnerModule],
  templateUrl: './exif-panel.component.html',
  styleUrl: './exif-panel.component.scss'
})
export class ExifPanelComponent implements OnChanges {
  @Input() assetId!: number;
  @Input() visible = false;
  @Output() closed = new EventEmitter<void>();

  @HostBinding('style.display')
  get hostDisplay(): string {
    return this.visible ? 'flex' : 'none';
  }

  exif: ExifMetadata | null = null;
  loading = false;

  private cache = new Map<number, ExifMetadata | null>();

  constructor(private assetService: AssetService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.visible && this.assetId != null && ('visible' in changes || 'assetId' in changes)) {
      if (this.cache.has(this.assetId)) {
        this.exif = this.cache.get(this.assetId) ?? null;
        this.loading = false;
      } else {
        this.loading = true;
        this.exif = null;
        this.assetService.getExifMetadata(this.assetId).subscribe({
          next: (data) => {
            this.cache.set(this.assetId, data);
            this.exif = data;
            this.loading = false;
          },
          error: () => {
            this.cache.set(this.assetId, null);
            this.loading = false;
          }
        });
      }
    }
  }

  close(): void {
    this.closed.emit();
  }
}
