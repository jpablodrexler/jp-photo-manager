import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { FolderNavComponent } from '../../folder-nav/folder-nav.component';
import { FolderPickerDialogData, FolderPickerDialogResult } from '../../../core/models/dialog.model';

@Component({
  selector: 'app-folder-picker-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    FolderNavComponent,
  ],
  templateUrl: './folder-picker-dialog.component.html',
  styleUrl: './folder-picker-dialog.component.scss',
})
export class FolderPickerDialogComponent {
  selectedFolder: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<FolderPickerDialogComponent, FolderPickerDialogResult | null>,
    @Inject(MAT_DIALOG_DATA) public data: FolderPickerDialogData,
  ) {}

  get title(): string {
    return this.data.mode === 'move' ? 'Move to Folder' : 'Copy to Folder';
  }

  get confirmLabel(): string {
    return this.data.mode === 'move' ? 'Move here' : 'Copy here';
  }

  get isConfirmDisabled(): boolean {
    return !this.selectedFolder || this.selectedFolder === this.data.sourceFolder;
  }

  onFolderSelected(path: string): void {
    this.selectedFolder = path;
  }

  confirm(): void {
    if (!this.isConfirmDisabled && this.selectedFolder) {
      this.dialogRef.close({ destinationFolder: this.selectedFolder });
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
