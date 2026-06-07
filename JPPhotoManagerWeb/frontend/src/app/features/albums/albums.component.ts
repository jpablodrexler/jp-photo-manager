import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { AlbumService } from '../../core/services/album.service';
import { AlbumSummary, AlbumFilterJson } from '../../core/models/album.model';

@Component({
  selector: 'app-albums',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatToolbarModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './albums.component.html',
  styleUrl: './albums.component.scss'
})
export class AlbumsComponent implements OnInit {

  albums: AlbumSummary[] = [];
  showCreateForm = false;
  newAlbumName = '';
  makeSmartAlbum = false;
  smartSearch = '';
  smartDateFrom: Date | null = null;
  smartDateTo: Date | null = null;
  smartMinRating = 0;

  constructor(
    private albumService: AlbumService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.albumService.getAlbums().subscribe({
      next: albums => (this.albums = albums),
      error: () => this.snackBar.open('Failed to load albums', 'Dismiss', { duration: 3000 })
    });
  }

  isSmartAlbum(album: AlbumSummary): boolean {
    return album.filterJson != null;
  }

  formatFilterSummary(filter: AlbumFilterJson): string {
    const parts: string[] = [];
    if (filter.search) parts.push(`Search: ${filter.search}`);
    if (filter.dateFrom) parts.push(`From: ${filter.dateFrom}`);
    if (filter.dateTo) parts.push(`To: ${filter.dateTo}`);
    if (filter.minRating) parts.push(`Min rating: ${filter.minRating}`);
    return parts.join(', ') || 'No criteria';
  }

  createAlbum(name: string): void {
    if (!name.trim()) return;
    let filterJson: AlbumFilterJson | undefined;
    if (this.makeSmartAlbum) {
      const f: AlbumFilterJson = {};
      if (this.smartSearch.trim()) f.search = this.smartSearch.trim();
      if (this.smartDateFrom) f.dateFrom = this.formatDate(this.smartDateFrom);
      if (this.smartDateTo) f.dateTo = this.formatDate(this.smartDateTo);
      if (this.smartMinRating > 0) f.minRating = this.smartMinRating;
      if (f.search || f.dateFrom || f.dateTo || f.minRating) {
        filterJson = f;
      }
    }
    this.albumService.createAlbum({ name: name.trim(), filterJson }).subscribe({
      next: album => {
        this.albums = [...this.albums, album];
        this.newAlbumName = '';
        this.showCreateForm = false;
        this.makeSmartAlbum = false;
        this.smartSearch = '';
        this.smartDateFrom = null;
        this.smartDateTo = null;
        this.smartMinRating = 0;
        this.snackBar.open('Album created', undefined, { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to create album', 'Dismiss', { duration: 3000 })
    });
  }

  deleteAlbum(album: AlbumSummary): void {
    this.albumService.deleteAlbum(album.albumId).subscribe({
      next: () => {
        this.albums = this.albums.filter(a => a.albumId !== album.albumId);
        this.snackBar.open('Album deleted', undefined, { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to delete album', 'Dismiss', { duration: 3000 })
    });
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
