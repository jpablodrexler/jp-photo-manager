import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ThumbnailComponent } from '../../../shared/components/thumbnail/thumbnail.component';
import { AlbumService } from '../../../core/services/album.service';
import { Album, AlbumFilterJson } from '../../../core/models/album.model';
import { EditAlbumFilterDialogComponent } from './edit-album-filter-dialog.component';

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
    MatCardModule,
    MatDialogModule,
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
    private snackBar: MatSnackBar,
    private dialog: MatDialog
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

  isSmartAlbum(): boolean {
    return this.album?.filterJson != null;
  }

  filterSummary(): string {
    const filter = this.album?.filterJson;
    if (!filter) return '';
    const parts: string[] = [];
    if (filter.search) parts.push(`Search: ${filter.search}`);
    if (filter.dateFrom) parts.push(`From: ${filter.dateFrom}`);
    if (filter.dateTo) parts.push(`To: ${filter.dateTo}`);
    if (filter.minRating) parts.push(`Min rating: ${filter.minRating}`);
    return parts.join(', ') || 'No criteria';
  }

  openEditFilterDialog(): void {
    const ref = this.dialog.open(EditAlbumFilterDialogComponent, {
      data: { filterJson: this.album!.filterJson ?? {} }
    });
    ref.afterClosed().subscribe((result: AlbumFilterJson | null | undefined) => {
      if (result !== null && result !== undefined) {
        this.albumService.updateAlbum(this.albumId, {
          name: this.album!.name,
          filterJson: result
        }).subscribe({
          next: () => this.loadPage(0),
          error: () => this.snackBar.open('Failed to update filter', 'Dismiss', { duration: 3000 })
        });
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
