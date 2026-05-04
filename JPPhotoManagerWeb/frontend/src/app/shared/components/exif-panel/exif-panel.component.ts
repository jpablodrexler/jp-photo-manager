import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AssetService } from '../../../core/services/asset.service';
import { ExifMetadata } from '../../../core/models/exif-metadata.model';

@Component({
  selector: 'app-exif-panel',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatIconModule],
  templateUrl: './exif-panel.component.html',
  styleUrl: './exif-panel.component.scss'
})
export class ExifPanelComponent implements OnChanges {
  @Input() assetId: number | null = null;

  exif: ExifMetadata | null = null;
  loading = false;

  constructor(private assetService: AssetService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['assetId'] && this.assetId != null) {
      this.loadExif(this.assetId);
    } else if (changes['assetId'] && this.assetId == null) {
      this.exif = null;
    }
  }

  private loadExif(assetId: number): void {
    this.loading = true;
    this.exif = null;
    this.assetService.getExifMetadata(assetId).subscribe({
      next: (data) => { this.exif = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
