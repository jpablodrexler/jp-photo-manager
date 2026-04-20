import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { AssetService } from '../../core/services/asset.service';
import { Asset } from '../../core/models/asset.model';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';

interface DuplicateGroup {
  assets: Asset[];
  keepIndex: number;
}

@Component({
  selector: 'app-duplicates',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatChipsModule,
    FileSizePipe
  ],
  templateUrl: './duplicates.component.html',
  styleUrl: './duplicates.component.scss'
})
export class DuplicatesComponent implements OnInit {

  groups: DuplicateGroup[] = [];
  loading = false;
  totalDuplicates = 0;

  constructor(
    private assetService: AssetService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDuplicates();
  }

  loadDuplicates(): void {
    this.loading = true;
    this.assetService.getDuplicatedAssets().subscribe({
      next: (duplicateGroups: Asset[][]) => {
        this.groups = duplicateGroups.map(assets => ({ assets, keepIndex: 0 }));
        this.totalDuplicates = duplicateGroups.reduce((sum, g) => sum + g.length - 1, 0);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load duplicates', 'Dismiss', { duration: 3000 });
      }
    });
  }

  markAsKeep(group: DuplicateGroup, index: number): void {
    group.keepIndex = index;
  }

  deleteGroup(group: DuplicateGroup): void {
    const toDelete = group.assets
      .filter((_, i) => i !== group.keepIndex)
      .map(a => a.assetId);

    this.assetService.deleteAssets(toDelete, true).subscribe({
      next: () => {
        this.groups = this.groups.filter(g => g !== group);
        this.snackBar.open(`Deleted ${toDelete.length} duplicate(s)`, undefined, { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to delete', 'Dismiss', { duration: 3000 })
    });
  }
}
