import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Asset } from '../../../core/models/asset.model';

@Component({
  selector: 'app-media-fullscreen-overlay',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './media-fullscreen-overlay.component.html',
  styleUrl: './media-fullscreen-overlay.component.scss',
})
export class MediaFullscreenOverlayComponent {
  @Input() track: Asset | null = null;
  @Input() currentTime = 0;
  @Input() duration = 0;
  @Input() isPlaying = false;

  @Output() closed = new EventEmitter<void>();
  @Output() seekRequested = new EventEmitter<number>();
  @Output() previous = new EventEmitter<void>();
  @Output() stopped = new EventEmitter<void>();
  @Output() playPauseToggled = new EventEmitter<void>();
  @Output() nextTrack = new EventEmitter<void>();

  formatTime(seconds: number): string {
    if (!isFinite(seconds) || isNaN(seconds)) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  onSeek(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.seekRequested.emit(Number(input.value));
  }
}
