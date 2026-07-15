import { AsyncPipe } from '@angular/common';
import { Component, DoCheck, inject, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subscription } from 'rxjs';
import { AssetService } from './core/services/asset.service';
import { AuthService } from './core/services/auth.service';
import { BackgroundSyncService } from './core/services/background-sync.service';
import { MediaPlayerService } from './core/services/media-player.service';
import { ThemeService } from './core/services/theme.service';
import { PreferenceService } from './core/services/preference.service';
import { CatalogNotification, CatalogState } from './core/models/catalog-notification.model';
import { AudioPlayerComponent } from './features/audio-player/audio-player.component';
import { VideoPlayerComponent } from './features/gallery/video-player/video-player.component';
import { MediaFullscreenOverlayComponent } from './shared/components/media-fullscreen-overlay/media-fullscreen-overlay.component';
import { AboutDialogComponent } from './shared/components/about-dialog/about-dialog.component';
import { AccentColorPickerComponent } from './shared/components/accent-color-picker/accent-color-picker.component';
import { CatalogProgressFooterComponent } from './shared/components/catalog-progress-footer/catalog-progress-footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    AudioPlayerComponent,
    VideoPlayerComponent,
    MediaFullscreenOverlayComponent,
    MatSnackBarModule,
    AccentColorPickerComponent,
    CatalogProgressFooterComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit, OnDestroy, DoCheck {
  title = 'JP Photo Manager';
  isMobile = false;
  private bpSub!: Subscription;
  private onlineListener = () => this.replayPendingMutations();

  readonly mediaPlayer = inject(MediaPlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly assetService = inject(AssetService);

  readonly isDark$ = this.themeService.isDark$;
  readonly accentColor$ = this.themeService.accentColor$;

  catalogState: CatalogState = 'idle';
  catalogPercentCompleted = 0;
  catalogStatusText = '';
  catalogLastCompletedAt: Date | null = null;

  private catalogEventSource?: EventSource;
  private wasLoggedIn = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private themeService: ThemeService,
    private preferenceService: PreferenceService,
    private backgroundSyncService: BackgroundSyncService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.themeService.init();
    this.bpSub = this.breakpointObserver.observe([Breakpoints.Handset]).subscribe(result => {
      this.isMobile = result.matches;
    });
    window.addEventListener('online', this.onlineListener);
    if (navigator.onLine) {
      this.replayPendingMutations();
    }
  }

  ngDoCheck(): void {
    const loggedIn = this.isLoggedIn;
    if (loggedIn && !this.wasLoggedIn) {
      this.connectCatalog();
    } else if (!loggedIn && this.wasLoggedIn) {
      this.disconnectCatalog();
    }
    this.wasLoggedIn = loggedIn;
  }

  ngOnDestroy(): void {
    this.bpSub.unsubscribe();
    window.removeEventListener('online', this.onlineListener);
    this.disconnectCatalog();
  }

  connectCatalog(): void {
    this.disconnectCatalog();
    this.catalogState = 'idle';
    this.catalogPercentCompleted = 0;
    this.catalogStatusText = '';

    this.catalogEventSource = this.assetService.observeCatalog();

    this.catalogEventSource.addEventListener('catalog', (event: MessageEvent) => {
      this.catalogState = 'running';
      const notification = JSON.parse(event.data as string) as CatalogNotification;
      this.catalogPercentCompleted = notification.percentCompleted;
      if (notification.folderPath) {
        this.catalogStatusText = notification.folderPath;
      } else if (notification.asset?.fileName) {
        this.catalogStatusText = notification.asset.fileName;
      }
    });

    this.catalogEventSource.addEventListener('catalog-done', () => {
      this.catalogState = 'idle';
      this.catalogLastCompletedAt = new Date();
    });

    this.catalogEventSource.onerror = () => {
      if (this.catalogState === 'running') {
        this.catalogState = 'idle';
      }
    };
  }

  private disconnectCatalog(): void {
    this.catalogEventSource?.close();
    this.catalogEventSource = undefined;
  }

  selectAccentColor(color: string): void {
    this.themeService.setAccentColor(color);
  }

  private replayPendingMutations(): void {
    this.backgroundSyncService.getPendingCount().then(count => {
      if (count > 0) {
        this.snackBar.open(`Syncing ${count} pending changes…`, undefined, { duration: 4000 });
        this.backgroundSyncService.replayQueue();
      }
    });
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  toggleTheme(): void {
    const newMode = this.themeService.toggle();
    if (this.authService.isLoggedIn()) {
      this.preferenceService.save(newMode).subscribe({
        error: () => {
          this.snackBar.open('Failed to save theme preference', 'Dismiss', { duration: 3000 });
        },
      });
    }
  }

  logout(): void {
    this.authService.logout().subscribe({ error: () => {} });
    this.router.navigateByUrl('/login');
  }

  openAbout(): void {
    this.dialog.open(AboutDialogComponent);
  }
}
