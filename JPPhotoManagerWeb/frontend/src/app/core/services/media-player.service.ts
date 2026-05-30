import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Asset } from '../models/asset.model';

@Injectable({ providedIn: 'root' })
export class MediaPlayerService {

  readonly currentTrack = signal<Asset | null>(null);
  readonly queue = signal<Asset[]>([]);
  readonly currentIndex = signal<number>(0);
  readonly isPlaying = signal<boolean>(false);
  readonly currentTime = signal<number>(0);
  readonly duration = signal<number>(0);
  readonly isVideoPlaying = signal<boolean>(false);
  readonly isAudioFullscreen = signal<boolean>(false);
  readonly videoStreamUrl = signal<string | null>(null);

  private audio = new Audio();
  private videoEl: HTMLVideoElement | null = null;

  private readonly VIDEO_EXTENSIONS = new Set(['.mp4', '.mov', '.mkv', '.avi', '.webm']);

  constructor(private http: HttpClient) {
    this.audio.addEventListener('timeupdate', () => {
      this.currentTime.set(this.audio.currentTime);
      this.duration.set(this.audio.duration || 0);
    });
    this.audio.addEventListener('ended', () => this.next());
    this.audio.addEventListener('play', () => this.isPlaying.set(true));
    this.audio.addEventListener('pause', () => this.isPlaying.set(false));
  }

  registerVideoElement(el: HTMLVideoElement | null): void {
    if (el) {
      this.videoEl = el;
      el.addEventListener('play', () => this.isPlaying.set(true));
      el.addEventListener('pause', () => this.isPlaying.set(false));
      el.addEventListener('timeupdate', () => {
        if (this.isVideoPlaying()) {
          this.currentTime.set(el.currentTime);
          this.duration.set(el.duration || 0);
        }
      });
      el.addEventListener('ended', () => this.next());
    } else {
      this.videoEl = null;
    }
  }

  isVideoAsset(asset: Asset): boolean {
    const name = asset.fileName.toLowerCase();
    const dotIdx = name.lastIndexOf('.');
    if (dotIdx === -1) return false;
    return this.VIDEO_EXTENSIONS.has(name.slice(dotIdx));
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
    if (this.isVideoPlaying()) {
      if (this.videoEl?.paused) {
        this.videoEl.play().catch(() => {});
      } else {
        this.videoEl?.pause();
      }
    } else {
      if (this.audio.paused) {
        this.audio.play().catch(() => {});
      } else {
        this.audio.pause();
      }
    }
  }

  stop(): void {
    this.audio.pause();
    this.audio.src = '';
    if (this.videoEl) {
      this.videoEl.pause();
      this.videoEl.src = '';
    }
    this.videoStreamUrl.set(null);
    this.isPlaying.set(false);
    this.isVideoPlaying.set(false);
    this.currentTrack.set(null);
    this.queue.set([]);
    this.currentTime.set(0);
    this.duration.set(0);
  }

  prev(): void {
    const currentTimeVal = this.isVideoPlaying()
      ? (this.videoEl?.currentTime ?? 0)
      : this.audio.currentTime;
    if (currentTimeVal > 3) {
      if (this.isVideoPlaying() && this.videoEl) {
        this.videoEl.currentTime = 0;
      } else {
        this.audio.currentTime = 0;
      }
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
      this.isVideoPlaying.set(false);
    }
  }

  seek(seconds: number): void {
    if (this.isVideoPlaying() && this.videoEl) {
      this.videoEl.currentTime = seconds;
    } else {
      this.audio.currentTime = seconds;
    }
  }

  private loadAndPlay(asset: Asset): void {
    this.currentTrack.set(asset);
    const url = `/api/assets/${asset.assetId}/stream`;
    if (this.isVideoAsset(asset)) {
      this.audio.pause();
      this.audio.src = '';
      this.videoStreamUrl.set(url);
      this.isVideoPlaying.set(true);
      if (this.videoEl) {
        this.videoEl.src = url;
        this.videoEl.load();
        this.videoEl.play().catch(() => {});
      }
    } else {
      if (this.videoEl) {
        this.videoEl.pause();
        this.videoEl.src = '';
      }
      this.videoStreamUrl.set(null);
      this.isVideoPlaying.set(false);
      this.audio.src = url;
      this.audio.load();
      this.audio.play().catch(() => {});
    }
  }
}
