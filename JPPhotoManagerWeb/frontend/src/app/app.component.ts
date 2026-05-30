import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDialog } from '@angular/material/dialog';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subscription } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { MediaPlayerService } from './core/services/media-player.service';
import { AudioPlayerComponent } from './features/audio-player/audio-player.component';
import { VideoPlayerComponent } from './features/gallery/video-player/video-player.component';
import { MediaFullscreenOverlayComponent } from './shared/components/media-fullscreen-overlay/media-fullscreen-overlay.component';
import { AboutDialogComponent } from './shared/components/about-dialog/about-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
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
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'JP Photo Manager';
  isMobile = false;
  private bpSub!: Subscription;

  readonly mediaPlayer = inject(MediaPlayerService);
  private readonly dialog = inject(MatDialog);

  constructor(
    private authService: AuthService,
    private router: Router,
    private breakpointObserver: BreakpointObserver
  ) {}

  ngOnInit(): void {
    this.bpSub = this.breakpointObserver.observe([Breakpoints.Handset]).subscribe(result => {
      this.isMobile = result.matches;
    });
  }

  ngOnDestroy(): void {
    this.bpSub.unsubscribe();
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  openAbout(): void {
    this.dialog.open(AboutDialogComponent);
  }
}
