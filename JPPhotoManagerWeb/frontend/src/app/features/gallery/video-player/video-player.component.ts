import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MediaPlayerService } from '../../../core/services/media-player.service';

@Component({
  selector: 'app-video-player',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './video-player.component.html',
  styleUrl: './video-player.component.scss',
})
export class VideoPlayerComponent implements AfterViewInit, OnDestroy {
  @ViewChild('videoEl') videoEl!: ElementRef<HTMLVideoElement>;

  readonly mediaPlayer = inject(MediaPlayerService);

  ngAfterViewInit(): void {
    this.mediaPlayer.registerVideoElement(this.videoEl.nativeElement);
  }

  ngOnDestroy(): void {
    this.mediaPlayer.registerVideoElement(null);
  }

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

  requestFullscreen(): void {
    this.videoEl.nativeElement.requestFullscreen().catch(() => {});
  }
}
