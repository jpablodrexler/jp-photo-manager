import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Asset } from '../models/asset.model';

@Injectable({ providedIn: 'root' })
export class AudioPlayerService {

  readonly currentTrack = signal<Asset | null>(null);
  readonly queue = signal<Asset[]>([]);
  readonly currentIndex = signal<number>(0);
  readonly isPlaying = signal<boolean>(false);
  readonly currentTime = signal<number>(0);
  readonly duration = signal<number>(0);

  private audio = new Audio();

  constructor(private http: HttpClient) {
    this.audio.addEventListener('timeupdate', () => {
      this.currentTime.set(this.audio.currentTime);
      this.duration.set(this.audio.duration || 0);
    });
    this.audio.addEventListener('ended', () => {
      this.next();
    });
    this.audio.addEventListener('play', () => this.isPlaying.set(true));
    this.audio.addEventListener('pause', () => this.isPlaying.set(false));
  }

  play(assets: Asset[], startIndex = 0): void {
    this.queue.set(assets);
    this.currentIndex.set(startIndex);
    this.loadAndPlay(assets[startIndex]);
  }

  loadFolder(folderPath: string): void {
    const params = new HttpParams()
      .set('folderPath', folderPath)
      .set('sort', 'FILE_NAME')
      .set('page', '0');
    this.http.get<{ items: Asset[] }>('/api/assets', { params }).subscribe({
      next: (data) => {
        const audioAssets = data.items.filter(a => a.fileType === 'AUDIO');
        if (audioAssets.length > 0) {
          this.play(audioAssets, 0);
        }
      },
    });
  }

  loadPlaylist(assetId: number): void {
    this.http.get<Asset[]>(`/api/audio/playlist/${assetId}`).subscribe({
      next: (assets) => {
        if (assets.length > 0) {
          this.play(assets, 0);
        }
      },
    });
  }

  togglePause(): void {
    if (this.audio.paused) {
      this.audio.play();
    } else {
      this.audio.pause();
    }
  }

  stop(): void {
    this.audio.pause();
    this.audio.currentTime = 0;
    this.isPlaying.set(false);
    this.currentTrack.set(null);
    this.queue.set([]);
  }

  prev(): void {
    if (this.audio.currentTime > 3) {
      this.audio.currentTime = 0;
      return;
    }
    const idx = this.currentIndex() - 1;
    if (idx >= 0) {
      this.currentIndex.set(idx);
      this.loadAndPlay(this.queue()[idx]);
    }
  }

  next(): void {
    const idx = this.currentIndex() + 1;
    const q = this.queue();
    if (idx < q.length) {
      this.currentIndex.set(idx);
      this.loadAndPlay(q[idx]);
    } else {
      this.isPlaying.set(false);
    }
  }

  seek(seconds: number): void {
    this.audio.currentTime = seconds;
  }

  private loadAndPlay(asset: Asset): void {
    this.currentTrack.set(asset);
    this.audio.src = `/api/assets/${asset.assetId}/stream`;
    this.audio.load();
    this.audio.play();
  }
}
