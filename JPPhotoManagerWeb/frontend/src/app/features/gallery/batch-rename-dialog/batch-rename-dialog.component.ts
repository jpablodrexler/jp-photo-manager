import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { of } from 'rxjs';
import { AssetService } from '../../../core/services/asset.service';
import { RenamePreview } from '../../../core/models/asset.model';

export interface BatchRenameDialogData {
  assetIds: number[];
  assetCount: number;
}

@Component({
  selector: 'app-batch-rename-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './batch-rename-dialog.component.html',
  styleUrl: './batch-rename-dialog.component.scss',
})
export class BatchRenameDialogComponent implements OnInit, OnDestroy {
  pattern = '';
  previews: RenamePreview[] = [];
  previewError: string | null = null;
  isApplying = false;
  readonly displayedColumns = ['oldName', 'newName'];

  private readonly patternChange$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private assetService: AssetService,
    private dialogRef: MatDialogRef<BatchRenameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BatchRenameDialogData,
  ) {}

  ngOnInit(): void {
    this.patternChange$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(p => {
        if (!p) {
          this.previews = [];
          this.previewError = null;
          return of(null);
        }
        return this.assetService.renameAssets(this.data.assetIds, p, false);
      }),
      takeUntil(this.destroy$),
    ).subscribe({
      next: result => {
        if (result) {
          this.previews = result.previews;
          this.previewError = null;
        }
      },
      error: () => {
        this.previewError = 'Invalid pattern or name collision.';
        this.previews = [];
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPatternChange(value: string): void {
    this.previewError = null;
    this.patternChange$.next(value);
  }

  get canApply(): boolean {
    return !!this.pattern && !this.previewError && !this.isApplying && this.previews.length > 0;
  }

  applyRename(): void {
    this.isApplying = true;
    this.assetService.renameAssets(this.data.assetIds, this.pattern, true).subscribe({
      next: () => {
        this.dialogRef.close({ success: true, count: this.data.assetIds.length });
      },
      error: (err: { error?: { message?: string } }) => {
        const message = err?.error?.message ?? 'Rename failed';
        this.dialogRef.close({ success: false, error: message });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
