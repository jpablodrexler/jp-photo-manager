import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Asset } from '../../../core/models/asset.model';
import { FileSizePipe } from '../../pipes/file-size.pipe';

@Component({
  selector: 'app-thumbnail',
  standalone: true,
  imports: [MatCardModule, MatIconModule, FileSizePipe],
  template: `
    <mat-card class="thumbnail-card" [class.selected]="selected">
      <img
        [src]="asset.thumbnailUrl"
        [alt]="asset.fileName"
        loading="lazy"
        (error)="onImgError($event)"
      />
      <div class="thumbnail-stars">
        @for (star of [1,2,3,4,5]; track star) {
          <mat-icon class="thumb-star" [class.filled]="rating >= star"
                    (click)="ratingChange.emit(star); $event.stopPropagation()">
            {{ rating >= star ? 'star' : 'star_border' }}
          </mat-icon>
        }
      </div>
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
      &.selected { outline: 3px solid #4caf50; }

      img {
        width: 100%;
        height: 150px;
        object-fit: contain;
        display: block;
        background: #1a1a1a;
      }
    }
    .thumbnail-stars {
      display: flex;
      justify-content: center;
      gap: 2px;
      padding: 4px 0;
    }
    .thumb-star {
      font-size: 16px;
      cursor: pointer;
      color: rgba(255,255,255,0.38);
      &.filled { color: #ffa726; }
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
      color: rgba(255,255,255,0.5);
    }
  `]
})
export class ThumbnailComponent {
  @Input({ required: true }) asset!: Asset;
  @Input() selected = false;
  @Input() rating = 0;
  @Output() ratingChange = new EventEmitter<number>();

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }
}
