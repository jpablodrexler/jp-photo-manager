import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AlbumSummary } from '../../../core/models/album.model';
import { AddToAlbumDialogData, AddToAlbumDialogResult } from '../../../core/models/dialog.model';

@Component({
  selector: 'app-add-to-album-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatRadioModule,
    MatInputModule,
    MatFormFieldModule,
    MatListModule,
    MatTooltipModule
  ],
  template: `
    <h2 mat-dialog-title>Add to Album</h2>
    <mat-dialog-content>
      @if (data.albums.length > 0) {
        <p>Select an existing album:</p>
        <mat-radio-group [(ngModel)]="selectedAlbumId" class="album-radio-group">
          @for (album of data.albums; track album.albumId) {
            <mat-radio-button
              [value]="album.albumId"
              [disabled]="isSmartAlbum(album)"
              [matTooltip]="isSmartAlbum(album) ? 'Smart album — managed automatically' : ''"
            >
              {{ album.name }} ({{ album.assetCount }} photos)
            </mat-radio-button>
          }
        </mat-radio-group>
        <p style="margin-top: 16px">Or create a new album:</p>
      } @else {
        <p>Create a new album:</p>
      }
      <mat-form-field appearance="outline" style="width: 100%">
        <mat-label>New album name</mat-label>
        <input matInput [(ngModel)]="newAlbumName" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="confirm()" [disabled]="!selectedAlbumId && !newAlbumName.trim()">
        Add
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.album-radio-group { display: flex; flex-direction: column; gap: 8px; }`]
})
export class AddToAlbumDialogComponent {
  selectedAlbumId: number | null = null;
  newAlbumName = '';

  constructor(
    public dialogRef: MatDialogRef<AddToAlbumDialogComponent, AddToAlbumDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: AddToAlbumDialogData
  ) {}

  isSmartAlbum(album: AlbumSummary): boolean {
    return album.filterJson != null;
  }

  confirm(): void {
    if (this.newAlbumName.trim()) {
      this.dialogRef.close({ albumId: null, newAlbumName: this.newAlbumName.trim() });
    } else if (this.selectedAlbumId) {
      this.dialogRef.close({ albumId: this.selectedAlbumId, newAlbumName: null });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
