import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AudioPlayerService } from '../../core/services/audio-player.service';

@Component({
  selector: 'app-audio-player',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './audio-player.component.html',
  styleUrl: './audio-player.component.scss',
})
export class AudioPlayerComponent {
  readonly audioPlayer = inject(AudioPlayerService);

  formatTime(seconds: number): string {
    if (!isFinite(seconds) || isNaN(seconds)) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  onSeek(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.audioPlayer.seek(Number(input.value));
  }
}
