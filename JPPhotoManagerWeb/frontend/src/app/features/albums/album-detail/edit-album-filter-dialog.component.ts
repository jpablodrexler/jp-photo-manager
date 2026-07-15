import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { AlbumFilterJson, EditAlbumFilterDialogData } from '../../../core/models/album.model';

@Component({
  selector: 'app-edit-album-filter-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>Edit Smart Album Filter</h2>
    <mat-dialog-content>
      <div class="filter-fields">
        <mat-form-field appearance="outline" style="width: 100%">
          <mat-label>Search</mat-label>
          <input matInput [(ngModel)]="search" placeholder="Filename keyword" />
        </mat-form-field>
        <mat-form-field appearance="outline" style="width: 100%">
          <mat-label>Date from</mat-label>
          <input matInput [matDatepicker]="pickerFrom" [(ngModel)]="dateFrom" />
          <mat-datepicker-toggle matIconSuffix [for]="pickerFrom"></mat-datepicker-toggle>
          <mat-datepicker #pickerFrom></mat-datepicker>
        </mat-form-field>
        <mat-form-field appearance="outline" style="width: 100%">
          <mat-label>Date to</mat-label>
          <input matInput [matDatepicker]="pickerTo" [(ngModel)]="dateTo" />
          <mat-datepicker-toggle matIconSuffix [for]="pickerTo"></mat-datepicker-toggle>
          <mat-datepicker #pickerTo></mat-datepicker>
        </mat-form-field>
        <div class="filter-rating">
          @for (star of [1,2,3,4,5]; track star) {
            <mat-icon class="filter-star" [class.active]="minRating >= star"
                      (click)="minRating = (minRating === star ? 0 : star)">
              {{ minRating >= star ? 'star' : 'star_border' }}
            </mat-icon>
          }
          <span class="filter-rating-label">{{ minRating > 0 ? minRating + '★ +' : 'Any rating' }}</span>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="confirm()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .filter-fields { display: flex; flex-direction: column; gap: 8px; min-width: 320px; }
    .filter-rating { display: flex; align-items: center; gap: 2px; }
    .filter-star { cursor: pointer; color: rgba(0,0,0,0.3); }
    .filter-star.active { color: #f5a623; }
    .filter-rating-label { margin-left: 8px; font-size: 0.85em; }
  `]
})
export class EditAlbumFilterDialogComponent {
  search: string;
  dateFrom: Date | null;
  dateTo: Date | null;
  minRating: number;

  constructor(
    public dialogRef: MatDialogRef<EditAlbumFilterDialogComponent, AlbumFilterJson | null>,
    @Inject(MAT_DIALOG_DATA) public data: EditAlbumFilterDialogData
  ) {
    this.search = data.filterJson.search ?? '';
    this.dateFrom = data.filterJson.dateFrom ? new Date(data.filterJson.dateFrom) : null;
    this.dateTo = data.filterJson.dateTo ? new Date(data.filterJson.dateTo) : null;
    this.minRating = data.filterJson.minRating ?? 0;
  }

  confirm(): void {
    const f: AlbumFilterJson = {};
    if (this.search.trim()) f.search = this.search.trim();
    if (this.dateFrom) f.dateFrom = this.dateFrom.toISOString().split('T')[0];
    if (this.dateTo) f.dateTo = this.dateTo.toISOString().split('T')[0];
    if (this.minRating > 0) f.minRating = this.minRating;
    this.dialogRef.close(f);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
