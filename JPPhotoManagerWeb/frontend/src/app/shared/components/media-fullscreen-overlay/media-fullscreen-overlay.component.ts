import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MediaPlayerService } from '../../../core/services/media-player.service';

@Component({
  selector: 'app-media-fullscreen-overlay',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './media-fullscreen-overlay.component.html',
  styleUrl: './media-fullscreen-overlay.component.scss',
})
export class MediaFullscreenOverlayComponent {
  readonly mediaPlayer = inject(MediaPlayerService);

  formatTime(seconds: number): string {
    if (!isFinite(seconds) || isNaN(seconds)) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  onSeek(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.mediaPlayer.seek(Number(input.value));
  }
}
