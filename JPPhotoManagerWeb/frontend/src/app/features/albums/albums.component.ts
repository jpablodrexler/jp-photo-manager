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
import { AlbumService } from '../../core/services/album.service';
import { AlbumSummary } from '../../core/models/album.model';

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
    MatSnackBarModule
  ],
  templateUrl: './albums.component.html',
  styleUrl: './albums.component.scss'
})
export class AlbumsComponent implements OnInit {

  albums: AlbumSummary[] = [];
  showCreateForm = false;
  newAlbumName = '';

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

  createAlbum(name: string): void {
    if (!name.trim()) return;
    this.albumService.createAlbum({ name: name.trim() }).subscribe({
      next: album => {
        this.albums = [...this.albums, album];
        this.newAlbumName = '';
        this.showCreateForm = false;
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
}
