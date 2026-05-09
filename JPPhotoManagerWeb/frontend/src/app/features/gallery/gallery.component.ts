import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { FolderNavComponent } from '../folder-nav/folder-nav.component';
import { ThumbnailComponent } from '../../shared/components/thumbnail/thumbnail.component';
import { ExifPanelComponent } from '../../shared/components/exif-panel/exif-panel.component';
import { AssetService } from '../../core/services/asset.service';
import { AlbumService } from '../../core/services/album.service';
import { Asset, SortCriteria } from '../../core/models/asset.model';
import { AlbumSummary } from '../../core/models/album.model';
import { PaginatedData } from '../../core/models/paginated-data.model';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';
import { DropZoneComponent } from './drop-zone/drop-zone.component';
import { AddToAlbumDialogComponent } from './add-to-album-dialog/add-to-album-dialog.component';

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
    MatProgressBarModule,
    ScrollingModule,
    FolderNavComponent,
    ThumbnailComponent,
    ExifPanelComponent,
    FileSizePipe,
    DropZoneComponent
  ],
  templateUrl: './gallery.component.html',
  styleUrl: './gallery.component.scss'
})
export class GalleryComponent implements OnInit, OnDestroy {

  @ViewChild('scrollSentinel') private sentinel!: ElementRef<HTMLDivElement>;
  private observer: IntersectionObserver | null = null;

  currentFolder: string = '';
  viewMode: ViewMode = 'thumbnails';
  sortCriteria: SortCriteria = 'FILE_NAME';

  assets: Asset[] = [];
  selectedAssets: Set<number> = new Set();
  currentViewerIndex = 0;
  viewerZoom = 1;
  userAlbums: AlbumSummary[] = [];

  pageIndex = 0;
  totalItems = 0;
  isLoading = false;
  allLoaded = false;

  statusMessage = '';
  showExifPanel = false;

  readonly sortOptions: { value: SortCriteria; label: string }[] = [
    { value: 'FILE_NAME', label: 'File Name' },
    { value: 'FILE_SIZE', label: 'File Size' },
    { value: 'FILE_CREATION_DATE_TIME', label: 'Date Created' },
    { value: 'FILE_MODIFICATION_DATE_TIME', label: 'Date Modified' },
    { value: 'THUMBNAIL_CREATION_DATE_TIME', label: 'Date Cataloged' }
  ];

  constructor(
    private assetService: AssetService,
    private albumService: AlbumService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.disconnectObserver();
  }

  onFolderSelected(folderPath: string): void {
    this.currentFolder = folderPath;
    this.assets = [];
    this.pageIndex = 0;
    this.isLoading = false;
    this.allLoaded = false;
    this.selectedAssets.clear();
    this.disconnectObserver();
    this.albumService.getAlbums().subscribe({
      next: albums => (this.userAlbums = albums),
      error: () => {}
    });
    Promise.resolve().then(() => {
      this.setupSentinelObserver();
      this.loadNextPage();
    });
  }

  loadNextPage(): void {
    if (this.isLoading || this.allLoaded || !this.currentFolder) return;
    this.isLoading = true;
    this.assetService.getAssets(this.currentFolder, this.pageIndex, this.sortCriteria)
      .subscribe({
        next: (data: PaginatedData<Asset>) => {
          this.assets = [...this.assets, ...data.items];
          this.totalItems = data.totalItems;
          this.pageIndex++;
          this.allLoaded = this.pageIndex >= data.totalPages;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open('Failed to load assets', 'Dismiss', { duration: 3000 });
        }
      });
  }

  setupSentinelObserver(): void {
    if (!this.sentinel?.nativeElement) return;
    this.observer = new IntersectionObserver(
      (entries) => { if (entries[0].isIntersecting) this.loadNextPage(); },
      { threshold: 0.1 }
    );
    this.observer.observe(this.sentinel.nativeElement);
  }

  disconnectObserver(): void {
    this.observer?.disconnect();
    this.observer = null;
  }

  loadAssets(): void {
    this.assets = [];
    this.pageIndex = 0;
    this.isLoading = false;
    this.allLoaded = false;
    this.disconnectObserver();
    Promise.resolve().then(() => {
      this.setupSentinelObserver();
      this.loadNextPage();
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
    this.showExifPanel = false;
  }

  closeViewer(): void {
    this.viewMode = 'thumbnails';
    this.showExifPanel = false;
  }

  toggleExifPanel(): void {
    this.showExifPanel = !this.showExifPanel;
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

  onUploadComplete(): void {
    this.loadAssets();
  }

  onSortChange(): void {
    this.assets = [];
    this.pageIndex = 0;
    this.isLoading = false;
    this.allLoaded = false;
    this.disconnectObserver();
    Promise.resolve().then(() => {
      this.setupSentinelObserver();
      this.loadNextPage();
    });
  }

  downloadSelected(): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;

    const snackRef = this.snackBar.open('Preparing download…', undefined, { duration: 0 });
    this.assetService.downloadAssets(ids).subscribe({
      next: blob => {
        snackRef.dismiss();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'photos.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      },
      error: () => {
        snackRef.dismiss();
        this.snackBar.open('Failed to download assets', 'Dismiss', { duration: 3000 });
      }
    });
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

  addToAlbum(asset: Asset): void {
    const dialogRef = this.dialog.open(AddToAlbumDialogComponent, {
      width: '400px',
      data: { albums: this.userAlbums }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (!result) return;
      if (result.newAlbumName) {
        this.albumService.createAlbum({ name: result.newAlbumName }).subscribe({
          next: album => {
            this.userAlbums = [...this.userAlbums, album];
            this.albumService.addAssets(album.albumId, [asset.assetId]).subscribe({
              next: () => this.snackBar.open(`Added to "${album.name}"`, undefined, { duration: 2000 }),
              error: () => this.snackBar.open('Failed to add to album', 'Dismiss', { duration: 3000 })
            });
          },
          error: () => this.snackBar.open('Failed to create album', 'Dismiss', { duration: 3000 })
        });
      } else if (result.albumId) {
        this.albumService.addAssets(result.albumId, [asset.assetId]).subscribe({
          next: () => {
            const album = this.userAlbums.find(a => a.albumId === result.albumId);
            this.snackBar.open(`Added to "${album?.name ?? 'album'}"`, undefined, { duration: 2000 });
          },
          error: () => this.snackBar.open('Failed to add to album', 'Dismiss', { duration: 3000 })
        });
      }
    });
  }

  get currentViewerAsset(): Asset | undefined {
    return this.assets[this.currentViewerIndex];
  }

  get selectedCount(): number {
    return this.selectedAssets.size;
  }
}
