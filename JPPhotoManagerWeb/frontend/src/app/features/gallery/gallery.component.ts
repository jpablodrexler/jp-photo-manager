import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { FolderNavComponent } from '../folder-nav/folder-nav.component';
import { ThumbnailComponent } from '../../shared/components/thumbnail/thumbnail.component';
import { AssetService } from '../../core/services/asset.service';
import { Asset, SortCriteria } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';

type ViewMode = 'thumbnails' | 'viewer';

@Component({
  selector: 'app-gallery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatCheckboxModule,
    MatMenuModule,
    MatSnackBarModule,
    MatDialogModule,
    FolderNavComponent,
    ThumbnailComponent,
    FileSizePipe
  ],
  templateUrl: './gallery.component.html',
  styleUrl: './gallery.component.scss'
})
export class GalleryComponent implements OnInit {

  currentFolder: string = '';
  viewMode: ViewMode = 'thumbnails';
  sortCriteria: SortCriteria = 'FILE_NAME';

  assets: Asset[] = [];
  selectedAssets: Set<number> = new Set();
  currentViewerIndex = 0;
  viewerZoom = 1;

  pageIndex = 0;
  totalPages = 0;
  totalItems = 0;

  statusMessage = '';

  readonly sortOptions: { value: SortCriteria; label: string }[] = [
    { value: 'FILE_NAME', label: 'File Name' },
    { value: 'FILE_SIZE', label: 'File Size' },
    { value: 'FILE_CREATION_DATE_TIME', label: 'Date Created' },
    { value: 'FILE_MODIFICATION_DATE_TIME', label: 'Date Modified' },
    { value: 'THUMBNAIL_CREATION_DATE_TIME', label: 'Date Cataloged' }
  ];

  constructor(
    private assetService: AssetService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
  }

  onFolderSelected(folderPath: string): void {
    this.currentFolder = folderPath;
    this.pageIndex = 0;
    this.selectedAssets.clear();
    this.loadAssets();
  }

  loadAssets(): void {
    if (!this.currentFolder) return;

    this.assetService.getAssets(this.currentFolder, this.pageIndex, this.sortCriteria)
      .subscribe({
        next: (data: PaginatedData<Asset>) => {
          this.assets = data.items;
          this.totalPages = data.totalPages;
          this.totalItems = data.totalItems;
        },
        error: () => this.snackBar.open('Failed to load assets', 'Dismiss', { duration: 3000 })
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

  openViewer(index: number): void {
    this.currentViewerIndex = index;
    this.viewMode = 'viewer';
    this.viewerZoom = 1;
  }

  closeViewer(): void {
    this.viewMode = 'thumbnails';
  }

  viewerPrev(): void {
    if (this.currentViewerIndex > 0) {
      this.currentViewerIndex--;
      this.viewerZoom = 1;
    }
  }

  viewerNext(): void {
    if (this.currentViewerIndex < this.assets.length - 1) {
      this.currentViewerIndex++;
      this.viewerZoom = 1;
    }
  }

  zoomIn(): void {
    this.viewerZoom = Math.min(this.viewerZoom + 0.25, 4);
  }

  zoomOut(): void {
    this.viewerZoom = Math.max(this.viewerZoom - 0.25, 0.25);
  }

  prevPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.loadAssets();
    }
  }

  nextPage(): void {
    if (this.pageIndex < this.totalPages - 1) {
      this.pageIndex++;
      this.loadAssets();
    }
  }

  onSortChange(): void {
    this.pageIndex = 0;
    this.loadAssets();
  }

  deleteSelected(deleteFiles: boolean): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;

    this.assetService.deleteAssets(ids, deleteFiles).subscribe({
      next: () => {
        this.selectedAssets.clear();
        this.loadAssets();
        this.snackBar.open(`Deleted ${ids.length} asset(s)`, undefined, { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to delete assets', 'Dismiss', { duration: 3000 })
    });
  }

  get currentViewerAsset(): Asset | undefined {
    return this.assets[this.currentViewerIndex];
  }

  get selectedCount(): number {
    return this.selectedAssets.size;
  }
}
