import { AsyncPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
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
import { AuthService } from './core/services/auth.service';
import { BackgroundSyncService } from './core/services/background-sync.service';
import { MediaPlayerService } from './core/services/media-player.service';
import { ThemeService } from './core/services/theme.service';
import { PreferenceService } from './core/services/preference.service';
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
export class AppComponent implements OnInit, OnDestroy {
  title = 'JP Photo Manager';
  isMobile = false;
  private bpSub!: Subscription;
  private onlineListener = () => this.replayPendingMutations();

  readonly mediaPlayer = inject(MediaPlayerService);
  private readonly dialog = inject(MatDialog);

  readonly isDark$ = this.themeService.isDark$;

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

  ngOnDestroy(): void {
    this.bpSub.unsubscribe();
    window.removeEventListener('online', this.onlineListener);
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
      this.preferenceService.save(newMode);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  openAbout(): void {
    this.dialog.open(AboutDialogComponent);
  }
}
