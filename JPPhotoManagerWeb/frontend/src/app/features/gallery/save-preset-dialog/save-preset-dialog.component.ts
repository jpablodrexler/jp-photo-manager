import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-save-preset-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Save Filter Preset</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" style="width: 100%">
        <mat-label>Preset name</mat-label>
        <input matInput [(ngModel)]="name" (keydown.enter)="confirm()" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="confirm()" [disabled]="!name.trim()">Save</button>
    </mat-dialog-actions>
  `
})
export class SavePresetDialogComponent {
  name = '';

  constructor(private dialogRef: MatDialogRef<SavePresetDialogComponent, string>) {}

  confirm(): void {
    if (this.name.trim()) {
      this.dialogRef.close(this.name.trim());
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
