import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ThumbnailComponent } from '../../../shared/components/thumbnail/thumbnail.component';
import { AlbumService } from '../../../core/services/album.service';
import { Album } from '../../../core/models/album.model';

@Component({
  selector: 'app-album-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    ThumbnailComponent
  ],
  templateUrl: './album-detail.component.html',
  styleUrl: './album-detail.component.scss'
})
export class AlbumDetailComponent implements OnInit {

  album: Album | null = null;
  albumId = 0;
  currentPage = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private albumService: AlbumService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.albumId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.currentPage = page;
    this.albumService.getAlbum(this.albumId, page).subscribe({
      next: album => (this.album = album),
      error: () => {
        this.snackBar.open('Failed to load album', 'Dismiss', { duration: 3000 });
        this.router.navigate(['/albums']);
      }
    });
  }

  removeAsset(assetId: number): void {
    this.albumService.removeAssets(this.albumId, [assetId]).subscribe({
      next: () => {
        this.snackBar.open('Removed from album', undefined, { duration: 2000 });
        this.loadPage(this.currentPage);
      },
      error: () => this.snackBar.open('Failed to remove asset', 'Dismiss', { duration: 3000 })
    });
  }

  get totalPages(): number {
    return this.album?.assets.totalPages ?? 0;
  }
}
