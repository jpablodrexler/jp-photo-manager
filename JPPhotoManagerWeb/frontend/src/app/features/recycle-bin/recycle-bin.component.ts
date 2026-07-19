import { Component, OnInit } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RecycleBinService } from '../../core/services/recycle-bin.service';
import { ThumbnailComponent } from '../../shared/components/thumbnail/thumbnail.component';
import { Asset } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';

@Component({
  selector: 'app-recycle-bin',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    ThumbnailComponent
  ],
  templateUrl: './recycle-bin.component.html',
  styleUrl: './recycle-bin.component.scss'
})
export class RecycleBinComponent implements OnInit {

  assets: Asset[] = [];
  selectedAssets = new Set<number>();
  pageIndex = 0;
  totalPages = 0;
  totalItems = 0;

  constructor(
    private recycleBinService: RecycleBinService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.recycleBinService.getRecycleBin(page).subscribe({
      next: (data: PaginatedData<Asset>) => {
        this.assets = data.items;
        this.pageIndex = data.pageIndex;
        this.totalPages = data.totalPages;
        this.totalItems = Number(data.totalItems);
      },
      error: () => this.snackBar.open('Failed to load recycle bin', 'Dismiss', { duration: 3000 })
    });
  }

  toggleSelection(asset: Asset): void {
    if (this.selectedAssets.has(asset.assetId)) {
      this.selectedAssets.delete(asset.assetId);
    } else {
      this.selectedAssets.add(asset.assetId);
    }
  }

  isSelected(asset: Asset): boolean {
    return this.selectedAssets.has(asset.assetId);
  }

  restoreSelected(): void {
    const ids = Array.from(this.selectedAssets);
    this.recycleBinService.restoreAssets(ids).subscribe({
      next: () => {
        this.snackBar.open('Restored successfully', undefined, { duration: 2000 });
        this.selectedAssets.clear();
        this.loadPage(0);
      },
      error: () => this.snackBar.open('Failed to restore assets', 'Dismiss', { duration: 3000 })
    });
  }

  purgeSelected(): void {
    const ids = Array.from(this.selectedAssets);
    this.recycleBinService.purgeAssets(ids).subscribe({
      next: () => {
        this.snackBar.open('Permanently deleted', undefined, { duration: 2000 });
        this.selectedAssets.clear();
        this.loadPage(0);
      },
      error: () => this.snackBar.open('Failed to delete assets', 'Dismiss', { duration: 3000 })
    });
  }

  purgeAll(): void {
    this.recycleBinService.purgeAll().subscribe({
      next: () => {
        this.snackBar.open('Recycle bin emptied', undefined, { duration: 2000 });
        this.loadPage(0);
      },
      error: () => this.snackBar.open('Failed to empty recycle bin', 'Dismiss', { duration: 3000 })
    });
  }

  get selectedCount(): number {
    return this.selectedAssets.size;
  }
}
