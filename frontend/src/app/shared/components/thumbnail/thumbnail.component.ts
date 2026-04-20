import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Asset } from '../../../core/models/asset.model';
import { FileSizePipe } from '../../pipes/file-size.pipe';

@Component({
  selector: 'app-thumbnail',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, FileSizePipe],
  template: `
    <mat-card class="thumbnail-card" [class.selected]="selected">
      <img
        [src]="asset.thumbnailUrl"
        [alt]="asset.fileName"
        loading="lazy"
        (error)="onImgError($event)"
      />
      <mat-card-content class="thumbnail-info">
        <span class="thumbnail-name" [title]="asset.fileName">{{ asset.fileName }}</span>
        <span class="thumbnail-size">{{ asset.fileSize | fileSize }}</span>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .thumbnail-card {
      cursor: pointer;
      padding: 0;
      overflow: hidden;
      transition: transform 0.15s, box-shadow 0.15s;

      &:hover { transform: scale(1.02); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
      &.selected { outline: 3px solid #3f51b5; }

      img {
        width: 100%;
        height: 150px;
        object-fit: cover;
        display: block;
      }
    }
    .thumbnail-info {
      padding: 6px 8px !important;
    }
    .thumbnail-name {
      display: block;
      font-size: 12px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .thumbnail-size {
      font-size: 11px;
      color: rgba(0,0,0,0.5);
    }
  `]
})
export class ThumbnailComponent {
  @Input({ required: true }) asset!: Asset;
  @Input() selected = false;

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }
}
