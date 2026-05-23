import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule, FormControl } from "@angular/forms";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatNativeDateModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatMenuModule } from "@angular/material/menu";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatSnackBar, MatSnackBarModule } from "@angular/material/snack-bar";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatChipsModule, MatChipInputEvent } from "@angular/material/chips";
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";
import { COMMA, ENTER } from "@angular/cdk/keycodes";
import { BreakpointObserver, Breakpoints } from "@angular/cdk/layout";
import { Subject, Subscription } from "rxjs";
import { debounceTime, distinctUntilChanged, takeUntil } from "rxjs/operators";
import { ScrollingModule } from "@angular/cdk/scrolling";
import { FolderNavComponent } from "../folder-nav/folder-nav.component";
import { ExifPanelComponent } from "../../shared/components/exif-panel/exif-panel.component";
import { FileSizePipe } from "../../shared/pipes/file-size.pipe";
import { AssetService } from "../../core/services/asset.service";
import { TagService } from "../../core/services/tag.service";
import { AlbumService } from "../../core/services/album.service";
import { SearchPresetService } from "../../core/services/search-preset.service";
import { Asset, SortCriteria } from "../../core/models/asset.model";
import { AlbumSummary } from "../../core/models/album.model";
import { SearchPreset } from "../../core/models/search-preset.model";
import { PaginatedData } from "../../core/models/paginated-data.model";
import { DropZoneComponent } from "./drop-zone/drop-zone.component";
import { AddToAlbumDialogComponent } from "./add-to-album-dialog/add-to-album-dialog.component";
import { SavePresetDialogComponent } from "./save-preset-dialog/save-preset-dialog.component";
import { BulkTagDialogComponent } from "./bulk-tag-dialog/bulk-tag-dialog.component";
import { FolderPickerDialogComponent } from "./folder-picker-dialog/folder-picker-dialog.component";
import { TimelineViewComponent } from "./timeline-view/timeline-view.component";
import { TimelineGroup } from "../../core/models/timeline-group.model";

type ViewMode = "thumbnails" | "viewer" | "slideshow";
type ViewType = "grid" | "timeline";

@Component({
  selector: "app-gallery",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatCheckboxModule,
    MatMenuModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressBarModule,
    MatSidenavModule,
    MatChipsModule,
    MatAutocompleteModule,
    ScrollingModule,
    FolderNavComponent,
    ExifPanelComponent,
    FileSizePipe,
    DropZoneComponent,
    TimelineViewComponent,
    FolderPickerDialogComponent,
  ],
  templateUrl: "./gallery.component.html",
  styleUrl: "./gallery.component.scss",
})
export class GalleryComponent implements OnInit, OnDestroy {
  @ViewChild("scrollSentinel") private sentinel!: ElementRef<HTMLDivElement>;
  private observer: IntersectionObserver | null = null;

  isMobile = false;
  sidenavOpen = true;
  private bpSub!: Subscription;

  currentFolder: string = "";
  viewMode: ViewMode = "thumbnails";
  viewType: ViewType = "grid";
  sortCriteria: SortCriteria = "FILE_NAME";

  searchTerm = "";
  dateFrom: Date | null = null;
  dateTo: Date | null = null;
  minRating = 0;
  selectedTags: string[] = [];
  tagSuggestions: string[] = [];
  tagFilterControl = new FormControl<string>('', { nonNullable: true });
  readonly tagSeparatorKeysCodes = [ENTER, COMMA] as const;
  private readonly searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  private readonly destroy$ = new Subject<void>();

  assets: Asset[] = [];
  timelineGroups: TimelineGroup[] = [];
  timelinePageIndex = 0;
  timelineAllLoaded = false;
  selectedAssets: Set<number> = new Set();
  currentViewerIndex = 0;
  viewerZoom = 1;
  slideshowInterval = 5;
  slideshowPlaying = false;
  private slideshowTimer: ReturnType<typeof setInterval> | null = null;
  slideshowResetTick = false;
  readonly intervalOptions = [3, 5, 10, 15];
  userAlbums: AlbumSummary[] = [];
  presets: SearchPreset[] = [];
  selectedPresetId: number | null = null;

  pageIndex = 0;
  totalItems = 0;
  isLoading = false;
  allLoaded = false;

  statusMessage = "";
  showExifPanel = false;

  readonly sortOptions: { value: SortCriteria; label: string }[] = [
    { value: "FILE_NAME", label: "File Name" },
    { value: "FILE_SIZE", label: "File Size" },
    { value: "FILE_CREATION_DATE_TIME", label: "Date Created" },
    { value: "FILE_MODIFICATION_DATE_TIME", label: "Date Modified" },
    { value: "THUMBNAIL_CREATION_DATE_TIME", label: "Date Cataloged" },
    { value: "RATING", label: "Rating: High → Low" },
  ];

  constructor(
    private assetService: AssetService,
    private tagService: TagService,
    private albumService: AlbumService,
    private searchPresetService: SearchPresetService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadAssets();
      });

    this.tagFilterControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(q => {
      if (q && q.length >= 1) {
        this.tagService.searchTags(q).subscribe(tags => {
          this.tagSuggestions = tags.filter(t => !this.selectedTags.includes(t));
        });
      } else {
        this.tagSuggestions = [];
      }
    });

    this.bpSub = this.breakpointObserver
      .observe([Breakpoints.Handset])
      .subscribe((result) => {
        this.isMobile = result.matches;
        this.sidenavOpen = !result.matches;
      });

    this.loadPresets();

    const folderParam = this.route.snapshot.queryParamMap.get('folder');
    if (folderParam) {
      this.onFolderSelected(folderParam);
    }
  }

  ngOnDestroy(): void {
    this.stopSlideshow();
    this.disconnectObserver();
    this.searchSubscription?.unsubscribe();
    this.bpSub.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }

  onFolderSelected(folderPath: string): void {
    this.currentFolder = folderPath;
    if (this.isMobile) {
      this.sidenavOpen = false;
    }
    this.clearFilters();
    this.selectedAssets.clear();
    this.disconnectObserver();
    this.albumService.getAlbums().subscribe({
      next: (albums) => (this.userAlbums = albums),
      error: () => {},
    });
    Promise.resolve().then(() => {
      if (this.viewType === 'timeline') {
        this.setupSentinelObserver();
        this.loadTimelinePage();
      } else {
        this.loadNextPage(true);
      }
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.searchSubject.next(value);
  }

  onDateChange(): void {
    this.pageIndex = 0;
    this.loadAssets();
  }

  onMinRatingChange(): void {
    this.pageIndex = 0;
    this.loadAssets();
  }

  rateAsset(asset: Asset, star: number): void {
    const newRating = asset.rating === star ? 0 : star;
    this.assetService.rateAsset(asset.assetId, newRating).subscribe({
      next: () => {
        asset.rating = newRating;
      },
      error: () =>
        this.snackBar.open("Failed to rate asset", "Dismiss", {
          duration: 3000,
        }),
    });
  }

  rateCurrentAsset(star: number): void {
    this.rateAsset(this.currentViewerAsset!, star);
  }

  clearFilters(): void {
    this.searchTerm = "";
    this.dateFrom = null;
    this.dateTo = null;
    this.minRating = 0;
    this.selectedTags = [];
    this.tagSuggestions = [];
    this.tagFilterControl.setValue('', { emitEvent: false });
    this.assets = [];
    this.pageIndex = 0;
    this.isLoading = false;
    this.allLoaded = false;
    this.timelineGroups = [];
    this.timelinePageIndex = 0;
    this.timelineAllLoaded = false;
  }

  loadNextPage(continueLoading = false): void {
    if (this.isLoading || this.allLoaded || !this.currentFolder) return;
    this.isLoading = true;
    const search = this.searchTerm.trim() || undefined;
    const dateFrom = this.dateFrom
      ? this.dateFrom.toISOString().substring(0, 10)
      : undefined;
    const dateTo = this.dateTo
      ? this.dateTo.toISOString().substring(0, 10)
      : undefined;
    const minRating = this.minRating > 0 ? this.minRating : undefined;
    const tags = this.selectedTags.length > 0 ? this.selectedTags : undefined;
    this.assetService
      .getAssets(
        this.currentFolder,
        this.pageIndex,
        this.sortCriteria,
        search,
        dateFrom,
        dateTo,
        minRating,
        tags,
      )
      .subscribe({
        next: (data: PaginatedData<Asset>) => {
          this.assets = [...this.assets, ...data.items];
          this.totalItems = data.totalItems;
          this.pageIndex++;
          this.allLoaded = this.pageIndex >= data.totalPages;
          this.isLoading = false;
          if (continueLoading && !this.allLoaded) {
            this.loadNextPage(true);
          }
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open("Failed to load assets", "Dismiss", {
            duration: 3000,
          });
        },
      });
  }

  trackByAssetId(_index: number, asset: Asset): number {
    return asset.assetId;
  }

  setupSentinelObserver(): void {
    if (!this.sentinel?.nativeElement) return;
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          this.loadTimelinePage();
        }
      },
      { threshold: 0.1 },
    );
    this.observer.observe(this.sentinel.nativeElement);
  }

  loadTimelinePage(): void {
    if (this.isLoading || this.timelineAllLoaded || !this.currentFolder) return;
    this.isLoading = true;
    const search = this.searchTerm.trim() || undefined;
    const dateFrom = this.dateFrom ? this.dateFrom.toISOString().substring(0, 10) : undefined;
    const dateTo = this.dateTo ? this.dateTo.toISOString().substring(0, 10) : undefined;
    const minRating = this.minRating > 0 ? this.minRating : undefined;
    this.assetService.getTimeline(this.currentFolder, this.timelinePageIndex, { search, dateFrom, dateTo, minRating })
      .subscribe({
        next: (data) => {
          this.timelineGroups = [...this.timelineGroups, ...data.items];
          this.timelinePageIndex++;
          this.timelineAllLoaded = this.timelinePageIndex >= data.totalPages;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open('Failed to load timeline', 'Dismiss', { duration: 3000 });
        },
      });
  }

  setViewType(type: ViewType): void {
    if (this.viewType === type) return;
    this.viewType = type;
    this.disconnectObserver();
    if (type === 'timeline') {
      this.timelineGroups = [];
      this.timelinePageIndex = 0;
      this.timelineAllLoaded = false;
      this.isLoading = false;
    } else {
      this.assets = [];
      this.pageIndex = 0;
      this.isLoading = false;
      this.allLoaded = false;
    }
    Promise.resolve().then(() => {
      if (type === 'timeline') {
        this.setupSentinelObserver();
        this.loadTimelinePage();
      } else {
        this.loadNextPage(true);
      }
    });
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
    this.timelineGroups = [];
    this.timelinePageIndex = 0;
    this.timelineAllLoaded = false;
    this.disconnectObserver();
    Promise.resolve().then(() => {
      if (this.viewType === 'timeline') {
        this.setupSentinelObserver();
        this.loadTimelinePage();
      } else {
        this.loadNextPage(true);
      }
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
    this.viewMode = "viewer";
    this.viewerZoom = 1;
    this.showExifPanel = false;
  }

  openViewerFromTimeline(asset: Asset): void {
    const flat = this.timelineGroups.flatMap(g => g.assets);
    const idx = flat.findIndex(a => a.assetId === asset.assetId);
    this.assets = flat;
    this.openViewer(idx >= 0 ? idx : 0);
  }

  closeViewer(): void {
    this.viewMode = "thumbnails";
    this.showExifPanel = false;
  }

  @HostListener("keydown", ["$event"])
  onKeyDown(event: KeyboardEvent): void {
    switch (event.key) {
      case "Escape":
        if (this.viewMode === "slideshow") {
          this.exitSlideshow();
          event.preventDefault();
        }
        break;
      case " ":
        if (this.viewMode === "slideshow") {
          this.toggleSlideshowPlay();
          event.preventDefault();
        }
        break;
      case "ArrowLeft":
        if (this.viewMode === "slideshow") {
          this.stepSlideshow(-1);
          event.preventDefault();
        } else if (this.viewMode === "viewer") {
          this.viewerPrev();
          event.preventDefault();
        }
        break;
      case "ArrowRight":
        if (this.viewMode === "slideshow") {
          this.stepSlideshow(1);
          event.preventDefault();
        } else if (this.viewMode === "viewer") {
          this.viewerNext();
          event.preventDefault();
        }
        break;
    }
  }

  startSlideshow(index: number): void {
    this.currentViewerIndex = index;
    this.viewerZoom = 1;
    this.viewMode = "slideshow";
    this.slideshowPlaying = true;
    this.slideshowTimer = setInterval(
      () => this.advanceSlideshow(),
      this.slideshowInterval * 1000,
    );
  }

  advanceSlideshow(): void {
    if (this.currentViewerIndex < this.assets.length - 1) {
      this.currentViewerIndex++;
      this.viewerZoom = 1;
      this.slideshowResetTick = !this.slideshowResetTick;
    } else {
      this.stopSlideshow();
      this.statusMessage = "Slideshow complete";
      setTimeout(() => {
        this.statusMessage = "";
      }, 3000);
    }
  }

  pauseSlideshow(): void {
    if (this.slideshowTimer !== null) {
      clearInterval(this.slideshowTimer);
      this.slideshowTimer = null;
      this.slideshowPlaying = false;
    }
  }

  resumeSlideshow(): void {
    this.slideshowPlaying = true;
    this.slideshowTimer = setInterval(
      () => this.advanceSlideshow(),
      this.slideshowInterval * 1000,
    );
  }

  toggleSlideshowPlay(): void {
    if (this.slideshowPlaying) {
      this.pauseSlideshow();
    } else {
      this.resumeSlideshow();
    }
  }

  stopSlideshow(): void {
    this.pauseSlideshow();
    this.slideshowPlaying = false;
  }

  exitSlideshow(): void {
    this.stopSlideshow();
    this.viewMode = "viewer";
  }

  stepSlideshow(direction: -1 | 1): void {
    this.pauseSlideshow();
    if (direction === -1) {
      this.viewerPrev();
    } else {
      this.viewerNext();
    }
  }

  onIntervalChange(): void {
    if (this.slideshowPlaying) {
      this.pauseSlideshow();
      this.resumeSlideshow();
    }
    this.slideshowResetTick = !this.slideshowResetTick;
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
      this.loadNextPage(true);
    });
  }

  downloadSelected(): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;

    const snackRef = this.snackBar.open("Preparing download…", undefined, {
      duration: 0,
    });
    this.assetService.downloadAssets(ids).subscribe({
      next: (blob) => {
        snackRef.dismiss();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "photos.zip";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      },
      error: () => {
        snackRef.dismiss();
        this.snackBar.open("Failed to download assets", "Dismiss", {
          duration: 3000,
        });
      },
    });
  }

  deleteSelected(deleteFiles: boolean): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;

    this.assetService.deleteAssets(ids, deleteFiles).subscribe({
      next: () => {
        this.selectedAssets.clear();
        this.loadAssets();
        this.snackBar.open(`Deleted ${ids.length} asset(s)`, undefined, {
          duration: 2000,
        });
      },
      error: () =>
        this.snackBar.open("Failed to delete assets", "Dismiss", {
          duration: 3000,
        }),
    });
  }

  addToAlbum(asset: Asset): void {
    const dialogRef = this.dialog.open(AddToAlbumDialogComponent, {
      width: "400px",
      data: { albums: this.userAlbums },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      if (result.newAlbumName) {
        this.albumService.createAlbum({ name: result.newAlbumName }).subscribe({
          next: (album) => {
            this.userAlbums = [...this.userAlbums, album];
            this.albumService
              .addAssets(album.albumId, [asset.assetId])
              .subscribe({
                next: () =>
                  this.snackBar.open(`Added to "${album.name}"`, undefined, {
                    duration: 2000,
                  }),
                error: () =>
                  this.snackBar.open("Failed to add to album", "Dismiss", {
                    duration: 3000,
                  }),
              });
          },
          error: () =>
            this.snackBar.open("Failed to create album", "Dismiss", {
              duration: 3000,
            }),
        });
      } else if (result.albumId) {
        this.albumService.addAssets(result.albumId, [asset.assetId]).subscribe({
          next: () => {
            const album = this.userAlbums.find(
              (a) => a.albumId === result.albumId,
            );
            this.snackBar.open(
              `Added to "${album?.name ?? "album"}"`,
              undefined,
              { duration: 2000 },
            );
          },
          error: () =>
            this.snackBar.open("Failed to add to album", "Dismiss", {
              duration: 3000,
            }),
        });
      }
    });
  }

  loadPresets(): void {
    this.searchPresetService
      .listPresets()
      .subscribe({ next: (p) => (this.presets = p) });
  }

  onPresetSelected(presetId: number | null): void {
    if (presetId === null) return;
    const preset = this.presets.find((p) => p.presetId === presetId);
    if (preset) this.applyPreset(preset);
  }

  applyPreset(preset: SearchPreset): void {
    this.searchTerm = preset.search ?? "";
    this.dateFrom = preset.dateFrom ? new Date(preset.dateFrom) : null;
    this.dateTo = preset.dateTo ? new Date(preset.dateTo) : null;
    this.minRating = preset.minRating ?? 0;
    this.pageIndex = 0;
    this.loadAssets();
  }

  saveCurrentFiltersAsPreset(): void {
    const dialogRef = this.dialog.open(SavePresetDialogComponent, {
      width: "360px",
    });
    dialogRef.afterClosed().subscribe((name) => {
      if (!name) return;
      const dateFrom = this.dateFrom
        ? this.dateFrom.toISOString().substring(0, 10)
        : undefined;
      const dateTo = this.dateTo
        ? this.dateTo.toISOString().substring(0, 10)
        : undefined;
      const req = {
        name,
        search: this.searchTerm.trim() || undefined,
        dateFrom,
        dateTo,
        minRating: this.minRating > 0 ? this.minRating : undefined,
      };
      this.searchPresetService.createPreset(req).subscribe({
        next: (preset) => {
          this.presets = [...this.presets, preset];
          this.snackBar.open("Preset saved", undefined, { duration: 2000 });
        },
      });
    });
  }

  deletePreset(preset: SearchPreset, event: Event): void {
    event.stopPropagation();
    this.searchPresetService.deletePreset(preset.presetId).subscribe({
      next: () => {
        this.presets = this.presets.filter(
          (p) => p.presetId !== preset.presetId,
        );
        if (this.selectedPresetId === preset.presetId) {
          this.selectedPresetId = null;
        }
        this.snackBar.open("Preset deleted", undefined, { duration: 2000 });
      },
    });
  }

  addTagFilter(event: MatChipInputEvent): void {
    const name = (event.value ?? '').trim().toLowerCase();
    event.chipInput.clear();
    this.tagFilterControl.setValue('', { emitEvent: false });
    if (!name || this.selectedTags.includes(name)) return;
    this.selectedTags = [...this.selectedTags, name];
    this.pageIndex = 0;
    this.loadAssets();
  }

  addTagFilterFromAutocomplete(event: MatAutocompleteSelectedEvent): void {
    const name = event.option.viewValue.toLowerCase();
    this.tagFilterControl.setValue('', { emitEvent: false });
    this.tagSuggestions = [];
    if (!name || this.selectedTags.includes(name)) return;
    this.selectedTags = [...this.selectedTags, name];
    this.pageIndex = 0;
    this.loadAssets();
  }

  removeTagFilter(name: string): void {
    this.selectedTags = this.selectedTags.filter(t => t !== name);
    this.pageIndex = 0;
    this.loadAssets();
  }

  openBulkTagDialog(): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;
    this.dialog.open(BulkTagDialogComponent, {
      width: '440px',
      data: { assetIds: ids },
    }).afterClosed().subscribe((changed) => {
      if (changed) this.loadAssets();
    });
  }

  moveSelectedAssets(mode: 'move' | 'copy'): void {
    const ids = Array.from(this.selectedAssets);
    if (ids.length === 0) return;
    this.dialog.open(FolderPickerDialogComponent, {
      width: '480px',
      data: { mode, assetCount: ids.length, sourceFolder: this.currentFolder },
    }).afterClosed().subscribe((result) => {
      if (!result) return;
      const verb = mode === 'move' ? 'Moving' : 'Copying';
      const progressRef = this.snackBar.open(`${verb}…`, undefined, { duration: 0 });
      this.assetService.moveAssets(ids, result.destinationFolder, mode === 'copy').subscribe({
        next: () => {
          progressRef.dismiss();
          const folderName = result.destinationFolder.split('/').filter(Boolean).pop() ?? result.destinationFolder;
          const doneVerb = mode === 'move' ? 'Moved' : 'Copied';
          this.snackBar.open(`${doneVerb} ${ids.length} asset(s) to ${folderName}`, 'OK', { duration: 4000 });
          this.selectedAssets.clear();
          this.loadAssets();
        },
        error: () => {
          progressRef.dismiss();
          const errorVerb = mode === 'move' ? 'move' : 'copy';
          this.snackBar.open(`Failed to ${errorVerb} assets`, 'OK', { duration: 4000 });
        },
      });
    });
  }

  get currentViewerAsset(): Asset | undefined {
    return this.assets[this.currentViewerIndex];
  }

  get selectedCount(): number {
    return this.selectedAssets.size;
  }
}
