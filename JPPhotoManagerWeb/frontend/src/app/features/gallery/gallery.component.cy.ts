import { mount } from 'cypress/angular';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { GalleryComponent } from './gallery.component';
import { AssetService } from '../../core/services/asset.service';
import { TagService } from '../../core/services/tag.service';
import { AlbumService } from '../../core/services/album.service';
import { FolderService } from '../../core/services/folder.service';
import { SearchPresetService } from '../../core/services/search-preset.service';
import { AudioPlayerService } from '../../core/services/audio-player.service';
import { Asset } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';
import { SearchPreset } from '../../core/models/search-preset.model';
import { TimelineGroup } from '../../core/models/timeline-group.model';


describe('GalleryComponent', () => {
  const mockAssets: Asset[] = [
    {
      assetId: 1,
      folderId: 1,
      folderPath: '/photos',
      fileName: 'sunset.jpg',
      fileSize: 1024000,
      thumbnailCreationDateTime: '2024-06-01T10:00:00',
      hash: 'abc123',
      thumbnailUrl: '/api/assets/1/thumbnail',
      imageUrl: '/api/assets/1/image',
      rating: 0,
      tags: [],
      fileType: 'IMAGE',
    },
    {
      assetId: 2,
      folderId: 1,
      folderPath: '/photos',
      fileName: 'beach.jpg',
      fileSize: 512000,
      thumbnailCreationDateTime: '2024-06-02T10:00:00',
      hash: 'def456',
      thumbnailUrl: '/api/assets/2/thumbnail',
      imageUrl: '/api/assets/2/image',
      rating: 0,
      tags: [],
      fileType: 'IMAGE',
    },
  ];

  const emptyPage: PaginatedData<Asset> = { items: [], pageIndex: 0, totalPages: 0, totalItems: 0 };

  function mountGallery(
    assetServiceOverrides: Partial<AssetService> = {},
    searchPresetServiceOverrides: Partial<SearchPresetService> = {},
    tagServiceOverrides: Partial<TagService> = {},
  ) {
    const emptyTimelinePage: PaginatedData<TimelineGroup> = { items: [], pageIndex: 0, totalPages: 0, totalItems: 0 };
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of(emptyPage)),
      getTimeline: cy.stub().returns(of(emptyTimelinePage)),
      catalogAssets: cy.stub(),
      deleteAssets: cy.stub().returns(of(undefined)),
      moveAssets: cy.stub().returns(of(true)),
      ...assetServiceOverrides,
    };

    const tagServiceStub: Partial<TagService> = {
      searchTags: cy.stub().returns(of([])),
      bulkAddTag: cy.stub().returns(of(undefined)),
      bulkRemoveTag: cy.stub().returns(of(undefined)),
      ...tagServiceOverrides,
    };

    const folderServiceStub: Partial<FolderService> = {
      getFolders: cy.stub().returns(of([])),
    };

    const albumServiceStub: Partial<AlbumService> = {
      getAlbums: cy.stub().returns(of([])),
    };

    const searchPresetServiceStub: Partial<SearchPresetService> = {
      listPresets: cy.stub().returns(of([])),
      createPreset: cy.stub().returns(of({})),
      deletePreset: cy.stub().returns(of(undefined)),
      ...searchPresetServiceOverrides,
    };

    const audioPlayerStub: Partial<AudioPlayerService> = {
      currentTrack: signal(null),
      isPlaying: signal(false),
      play: cy.stub() as unknown as AudioPlayerService['play'],
      loadFolder: cy.stub() as unknown as AudioPlayerService['loadFolder'],
      loadPlaylist: cy.stub() as unknown as AudioPlayerService['loadPlaylist'],
    };

    return cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
        { provide: TagService, useValue: tagServiceStub },
        { provide: FolderService, useValue: folderServiceStub },
        { provide: AlbumService, useValue: albumServiceStub },
        { provide: SearchPresetService, useValue: searchPresetServiceStub },
        { provide: AudioPlayerService, useValue: audioPlayerStub },
      ],
    }).then(result => ({ ...result, assetServiceStub, searchPresetServiceStub, tagServiceStub }));
  }

  it('should create the component', () => {
    mountGallery().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should start in thumbnails view mode', () => {
    mountGallery().then(({ fixture }) => {
      expect(fixture.componentInstance.viewMode).to.equal('thumbnails');
    });
  });

  it('ngOnInit_doesNotCallCatalogAssets', () => {
    const catalogAssets = cy.stub();
    mountGallery({ catalogAssets });
    cy.wrap(catalogAssets).should('not.have.been.called');
  });

  it('should not render a progress bar', () => {
    mountGallery();
    cy.get('mat-progress-bar').should('not.exist');
  });

  it('should load assets when a folder is selected', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.get('.asset-list-row').should('have.length', 2);
  });

  it('assetListRows_folderSelected_containThumbnailImgElements', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.get('.asset-list-row img.list-thumb').should('have.length', 2);
  });

  it('thumbnailImg_folderSelected_srcMatchesThumbnailUrl', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.get('.asset-list-row img.list-thumb').eq(0).should('have.attr', 'src', '/api/assets/1/thumbnail');
    cy.get('.asset-list-row img.list-thumb').eq(1).should('have.attr', 'src', '/api/assets/2/thumbnail');
  });

  it('should not call getAssets when no folder is selected', () => {
    const getAssets = cy.stub().returns(of(emptyPage));
    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.loadAssets();
    });
    cy.wrap(getAssets).should('not.have.been.called');
  });

  it('should toggle asset selection', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      fixture.detectChanges();

      component.toggleSelection(mockAssets[0]);
      expect(component.isSelected(mockAssets[0])).to.be.true;

      component.toggleSelection(mockAssets[0]);
      expect(component.isSelected(mockAssets[0])).to.be.false;
    });
  });

  it('should switch to viewer mode when openViewer is called', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      component.openViewer(1);
      expect(component.viewMode).to.equal('viewer');
      expect(component.currentViewerIndex).to.equal(1);
    });
  });

  it('should switch back to thumbnails mode when closeViewer is called', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.openViewer(0);
      component.closeViewer();
      expect(component.viewMode).to.equal('thumbnails');
    });
  });

  it('should navigate to previous asset in viewer', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      component.openViewer(1);
      component.viewerPrev();
      expect(component.currentViewerIndex).to.equal(0);
    });
  });

  it('should navigate to next asset in viewer', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      component.openViewer(0);
      component.viewerNext();
      expect(component.currentViewerIndex).to.equal(1);
    });
  });

  it('should not navigate before first asset in viewer', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      component.openViewer(0);
      component.viewerPrev();
      expect(component.currentViewerIndex).to.equal(0);
    });
  });

  it('should not navigate past last asset in viewer', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = mockAssets;
      component.openViewer(1);
      component.viewerNext();
      expect(component.currentViewerIndex).to.equal(1);
    });
  });

  it('should increase zoom within limit', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.zoomIn();
      expect(component.viewerZoom).to.equal(1.25);
    });
  });

  it('should decrease zoom within limit', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.viewerZoom = 1;
      component.zoomOut();
      expect(component.viewerZoom).to.equal(0.75);
    });
  });

  it('should reset page to 0 when sort changes', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.pageIndex = 3;
      component.onSortChange();
      expect(component.pageIndex).to.equal(0);
    });
  });

  it('should call deleteAssets with selected IDs', () => {
    const deleteAssets = cy.stub().returns(of(undefined));
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ deleteAssets, getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.selectedAssets.add(1);
      component.selectedAssets.add(2);
      component.deleteSelected(false);
      cy.wrap(deleteAssets).should('have.been.calledWith', [1, 2], false);
    });
  });

  it('should not call deleteAssets when no assets are selected', () => {
    const deleteAssets = cy.stub().returns(of(undefined));

    mountGallery({ deleteAssets }).then(({ fixture }) => {
      fixture.componentInstance.deleteSelected(false);
      cy.wrap(deleteAssets).should('not.have.been.called');
    });
  });

  // --- Virtual scrolling tests ---

  it('thumbnailsView_withMultiplePages_noPaginationBarRendered', () => {
    const getAssets = cy.stub().returns(of({ items: mockAssets, pageIndex: 0, totalPages: 3, totalItems: 6 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.loadNextPage();
      fixture.detectChanges();
    });

    cy.get('.pagination-bar').should('not.exist');
  });

  it('loadNextPage_afterPage0_appendsAssetsAndCallsGetAssetsWithPage1', () => {
    const page0: PaginatedData<Asset> = { items: mockAssets, pageIndex: 0, totalPages: 2, totalItems: 4 };
    const page1: PaginatedData<Asset> = { items: [...mockAssets], pageIndex: 1, totalPages: 2, totalItems: 4 };
    const getAssets = cy.stub()
      .onFirstCall().returns(of(page0))
      .onSecondCall().returns(of(page1));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.loadNextPage();
      component.loadNextPage();
      fixture.detectChanges();
    });

    cy.wrap(getAssets).should('have.been.calledTwice');
    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 1, 'FILE_NAME');
  });

  it('onFolderSelected_resetsAssetsAndCallsGetAssetsWithPage0', () => {
    const getAssets = cy.stub().returns(of({ items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.onFolderSelected('/new-folder');
      expect(component.assets).to.deep.equal([]);
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/new-folder', 0, 'FILE_NAME');
    cy.get('.asset-list-row').should('have.length', 2);
  });

  it('onSortChange_resetsAssetsAndCallsGetAssetsWithPage0', () => {
    const getAssets = cy.stub().returns(of({ items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.assets = [...mockAssets];
      component.sortCriteria = 'FILE_SIZE';
      component.onSortChange();
      expect(component.assets).to.deep.equal([]);
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_SIZE');
  });

  it('isLoading_true_progressBarVisible', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.isLoading = true;
      fixture.detectChanges();
    });

    cy.get('mat-progress-bar').should('exist');
  });

  it('allLoaded_true_endOfListVisible', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.allLoaded = true;
      fixture.detectChanges();
    });

    cy.get('.end-of-list').should('be.visible');
  });

  // --- Download tests ---

  it('downloadSelected_withSelectedAsset_callsDownloadAssetsWithCorrectIds', () => {
    const downloadAssets = cy.stub().returns(of(new Blob()));
    cy.stub(URL, 'createObjectURL').returns('blob:http://localhost/fake');
    cy.stub(URL, 'revokeObjectURL');

    mountGallery({ downloadAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.selectedAssets.add(1);
      component.downloadSelected();
      cy.wrap(downloadAssets).should('have.been.calledWith', [1]);
    });
  });

  it('downloadSelected_serviceReturnsError_showsFailedSnackBar', () => {
    const downloadAssets = cy.stub().returns(throwError(() => new Error('network error')));

    mountGallery({ downloadAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.selectedAssets.add(1);
      component.downloadSelected();
    });

    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to download assets');
  });

  it('loadNextPage_whenAllLoaded_doesNotCallGetAssets', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.allLoaded = true;
      component.loadNextPage();
    });

    cy.wrap(getAssets).should('not.have.been.called');
  });

  // --- Slideshow tests ---

  it('startSlideshow_fromThumbnailsToolbar_entersSlideshowModeAndShowsControls', () => {
    const threeAssets: Asset[] = [
      ...mockAssets,
      {
        assetId: 3, folderId: 1, folderPath: '/photos', fileName: 'mountain.jpg',
        fileSize: 768000, thumbnailCreationDateTime: '2024-06-03T10:00:00',
        hash: 'ghi789', thumbnailUrl: '/api/assets/3/thumbnail', imageUrl: '/api/assets/3/image',
        rating: 0, tags: [], fileType: 'IMAGE',
      },
    ];
    const getAssets = cy.stub().returns(of({ items: threeAssets, pageIndex: 0, totalPages: 1, totalItems: 3 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.loadNextPage();
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.get('[title="Start slideshow"]').click();
    cy.get('[title="Pause"]').should('exist');
    cy.get('.slideshow-progress-bar').should('exist');
  });

  it('advanceSlideshow_afterInterval_incrementsCurrentViewerIndex', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.startSlideshow(0);
      fixture.detectChanges();
      expect(component.currentViewerIndex).to.equal(0);
      component.advanceSlideshow();
      expect(component.currentViewerIndex).to.equal(1);
    });
  });

  it('pauseSlideshow_whilePlaying_stopsAdvancing', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.startSlideshow(0);
      component.pauseSlideshow();
      expect(component.slideshowPlaying).to.be.false;
      expect(component.slideshowTimer).to.be.null;
      expect(component.currentViewerIndex).to.equal(0);
    });
  });

  it('exitSlideshow_onEscapeKey_returnsToViewerMode', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.startSlideshow(0);
      fixture.detectChanges();
      component.onKeyDown(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(component.viewMode).to.equal('viewer');
    });
  });

  it('toggleSlideshowPlay_onSpaceKey_togglesPlayPause', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.startSlideshow(0);
      expect(component.slideshowPlaying).to.be.true;
      component.onKeyDown(new KeyboardEvent('keydown', { key: ' ' }));
      expect(component.slideshowPlaying).to.be.false;
      component.onKeyDown(new KeyboardEvent('keydown', { key: ' ' }));
      expect(component.slideshowPlaying).to.be.true;
    });
  });

  it('advanceSlideshow_atLastAsset_stopsSlideshowAndShowsCompleteMessage', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.startSlideshow(1); // last index
      component.advanceSlideshow(); // at last asset — should stop
      expect(component.slideshowPlaying).to.be.false;
      expect(component.statusMessage).to.equal('Slideshow complete');
    });
  });

  it('startSlideshow_fromViewerToolbar_keepsSameIndex', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.openViewer(1);
      expect(component.viewMode).to.equal('viewer');
      component.startSlideshow(component.currentViewerIndex);
      expect(component.viewMode).to.equal('slideshow');
      expect(component.currentViewerIndex).to.equal(1);
    });
  });

  // --- Search and filter tests ---

  it('loadNextPage_withSearchTerm_callsGetAssetsWithSearchParam', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.searchTerm = 'vacation';
      component.loadNextPage();
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', 'vacation', undefined, undefined);
  });

  it('onDateChange_withDateFrom_callsGetAssetsWithIsoDateString', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.dateFrom = new Date(2024, 5, 15, 12, 0, 0); // noon local — safe for all UTC offsets
      component.onDateChange();
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', undefined, '2024-06-15', undefined);
  });

  it('clearFilters_resetsFilterStateAndReloads', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.searchTerm = 'old-search';
      component.dateFrom = new Date('2024-01-01');
      component.dateTo = new Date('2024-12-31');
      component.clearFilters();
      component.loadAssets();

      expect(component.searchTerm).to.equal('');
      expect(component.dateFrom).to.be.null;
      expect(component.dateTo).to.be.null;
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', undefined, undefined, undefined);
  });

  it('onFolderSelected_clearsFilersThenLoadsAssets', () => {
    const getAssets = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 1, totalItems: 0 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.searchTerm = 'old-search';
      component.dateFrom = new Date('2024-01-01');
      component.onFolderSelected('/new-folder');

      expect(component.searchTerm).to.equal('');
      expect(component.dateFrom).to.be.null;
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/new-folder', 0, 'FILE_NAME', undefined, undefined, undefined);
  });

  // --- Responsive sidenav tests ---

  it('mobileViewport_sidenavInOverMode_toggleButtonVisible', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.isMobile = true;
      component.sidenavOpen = false;
      fixture.detectChanges();
    });

    cy.get('button[aria-label="Toggle folder panel"]').should('be.visible');
    cy.get('mat-sidenav.mat-drawer-over').should('exist');
  });

  it('desktopViewport_sidenavInSideMode_toggleButtonNotRendered', () => {
    const bpObs: Partial<BreakpointObserver> = {
      observe: cy.stub().returns(of({ matches: false, breakpoints: {} })),
    };

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
      catalogAssets: cy.stub(),
      deleteAssets: cy.stub().returns(of(undefined)),
      moveAssets: cy.stub().returns(of(true)),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
        { provide: FolderService, useValue: { getFolders: cy.stub().returns(of([])) } },
        { provide: AlbumService, useValue: { getAlbums: cy.stub().returns(of([])) } },
        { provide: BreakpointObserver, useValue: bpObs },
        { provide: SearchPresetService, useValue: { listPresets: cy.stub().returns(of([])), createPreset: cy.stub().returns(of({})), deletePreset: cy.stub().returns(of(undefined)) } },
        { provide: TagService, useValue: { searchTags: cy.stub().returns(of([])) } },
        { provide: AudioPlayerService, useValue: { currentTrack: signal(null), isPlaying: signal(false), play: cy.stub(), loadFolder: cy.stub(), loadPlaylist: cy.stub() } },
      ],
    });

    cy.get('button[aria-label="Toggle folder panel"]').should('not.exist');
    cy.get('mat-sidenav').should('have.class', 'mat-drawer-side');
    cy.get('app-folder-nav').should('exist');
  });

  it('mobileViewport_onFolderSelected_closesSidenav', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.isMobile = true;
      component.sidenavOpen = true;
      fixture.detectChanges();

      component.onFolderSelected('/photos');
      fixture.detectChanges();

      expect(component.sidenavOpen).to.be.false;
      cy.get('mat-sidenav').should('not.have.class', 'mat-drawer-opened');
    });
  });

  // --- Star rating tests ---

  it('rateAsset_callWithStar4_callsServiceAndUpdatesAssetRating', () => {
    const rateAsset = cy.stub().returns(of(undefined));

    mountGallery({ rateAsset }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [...mockAssets];
      component.rateAsset(mockAssets[0], 4);
      cy.wrap(rateAsset).should('have.been.calledWith', 1, 4);
    });
  });

  it('rateAsset_sameStarAsCurrentRating_togglesRatingToZero', () => {
    const ratedAsset: Asset = { ...mockAssets[0], rating: 4 };
    const rateAsset = cy.stub().returns(of(undefined));

    mountGallery({ rateAsset }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.assets = [ratedAsset, mockAssets[1]];
      component.rateAsset(ratedAsset, 4);
      cy.wrap(rateAsset).should('have.been.calledWith', 1, 0);
    });
  });

  it('onMinRatingChange_filterStar3_callsGetAssetsWithMinRating3', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.minRating = 3;
      component.onMinRatingChange();
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', undefined, undefined, undefined, 3);
  });

  it('onSortChange_ratingSortSelected_callsGetAssetsWithRatingSort', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.sortCriteria = 'RATING';
      component.onSortChange();
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'RATING');
  });

  // --- Search preset tests ---

  const mockPresets: SearchPreset[] = [
    { presetId: 1, name: 'Vacation 3-star', createdAt: '2024-06-01T00:00:00Z', search: 'vacation', minRating: 3 },
    { presetId: 2, name: '2024 photos', createdAt: '2024-06-02T00:00:00Z', dateFrom: '2024-01-01', dateTo: '2024-12-31' },
  ];

  it('presetDropdown_withTwoPresets_rendersBothPresetNames', () => {
    const listPresets = cy.stub().returns(of(mockPresets));

    mountGallery({}, { listPresets }).then(({ fixture }) => {
      fixture.detectChanges();
    });

    cy.get('mat-select.preset-select').click();
    cy.get('mat-option').should('contain', 'Vacation 3-star');
    cy.get('mat-option').should('contain', '2024 photos');
  });

  it('applyPreset_selectFirstPreset_populatesFilterFieldsAndCallsGetAssets', () => {
    const listPresets = cy.stub().returns(of(mockPresets));
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }, { listPresets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.applyPreset(mockPresets[0]);
      expect(component.searchTerm).to.equal('vacation');
      expect(component.minRating).to.equal(3);
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', 'vacation', undefined, undefined, 3);
  });

  it('applyPreset_presetWithDateRange_populatesDateFieldsAndCallsGetAssets', () => {
    const listPresets = cy.stub().returns(of(mockPresets));
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }, { listPresets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.applyPreset(mockPresets[1]);
      expect(component.dateFrom).to.not.be.null;
      expect(component.dateTo).to.not.be.null;
      expect(component.minRating).to.equal(0);
    });

    cy.wrap(getAssets).should('have.been.calledWith', '/photos', 0, 'FILE_NAME', undefined, '2024-01-01', '2024-12-31', undefined);
  });

  it('saveCurrentFiltersAsPreset_confirmDialog_callsCreatePresetAndAppendsToList', () => {
    const newPreset: SearchPreset = { presetId: 3, name: 'Birthday 2024', createdAt: '2024-06-03T00:00:00Z', search: 'birthday' };
    const createPreset = cy.stub().returns(of(newPreset));
    const listPresets = cy.stub().returns(of([]));

    mountGallery({}, { listPresets, createPreset }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.searchTerm = 'birthday';
      component.saveCurrentFiltersAsPreset();
    });

    cy.get('app-save-preset-dialog input[matInput]').type('Birthday 2024');
    cy.get('app-save-preset-dialog button').contains('Save').click();

    cy.wrap(createPreset).should('have.been.calledOnce');
    cy.wrap(createPreset).should('have.been.calledWithMatch', { name: 'Birthday 2024', search: 'birthday' });
  });

  it('deletePreset_clickCloseIcon_callsDeletePresetAndRemovesFromList', () => {
    const deletePreset = cy.stub().returns(of(undefined));
    const listPresets = cy.stub().returns(of(mockPresets));

    mountGallery({}, { listPresets, deletePreset }).then(({ fixture }) => {
      fixture.detectChanges();
    });

    cy.get('mat-select.preset-select').click();
    cy.get('mat-option').first().find('button.preset-delete-btn').click({ force: true });

    cy.wrap(deletePreset).should('have.been.calledWith', 1);
  });

  // --- Tag filter tests ---

  it('tagFilter_addTagViaInput_reloadsAssetsWithTagParam', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.addTagFilter({ value: 'vacation', chipInput: { clear: () => {} } } as any);
      expect(component.selectedTags).to.deep.equal(['vacation']);
      expect(component.pageIndex).to.equal(0);
    });

    cy.wrap(getAssets).should('have.been.called');
  });

  it('tagFilter_removeTagChip_reloadsAssetsWithoutTag', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.selectedTags = ['vacation', 'family'];
      component.removeTagFilter('vacation');
      expect(component.selectedTags).to.deep.equal(['family']);
    });

    cy.wrap(getAssets).should('have.been.called');
  });

  it('tagFilter_clearFilters_clearsSelectedTags', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.selectedTags = ['vacation'];
      component.clearFilters();
      expect(component.selectedTags).to.deep.equal([]);
    });
  });

  // --- Query-param pre-selection tests ---

  it('ngOnInit_withFolderQueryParam_callsOnFolderSelectedWithCorrectPath', () => {
    const getAssets = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 1, totalItems: 0 }));

    const activatedRouteStub = {
      snapshot: {
        queryParamMap: {
          get: (key: string) => key === 'folder' ? '/photos' : null,
        },
      },
    };

    const assetServiceStub: Partial<AssetService> = {
      getAssets,
      catalogAssets: cy.stub(),
      deleteAssets: cy.stub().returns(of(undefined)),
      moveAssets: cy.stub().returns(of(true)),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetServiceStub },
        { provide: TagService, useValue: { searchTags: cy.stub().returns(of([])) } },
        { provide: FolderService, useValue: { getFolders: cy.stub().returns(of([])) } },
        { provide: AlbumService, useValue: { getAlbums: cy.stub().returns(of([])) } },
        { provide: SearchPresetService, useValue: { listPresets: cy.stub().returns(of([])), createPreset: cy.stub().returns(of({})), deletePreset: cy.stub().returns(of(undefined)) } },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: AudioPlayerService, useValue: { currentTrack: signal(null), isPlaying: signal(false), play: cy.stub(), loadFolder: cy.stub(), loadPlaylist: cy.stub() } },
      ],
    }).then(({ fixture }) => {
      fixture.detectChanges();
      cy.wrap(fixture.componentInstance).should('have.property', 'currentFolder', '/photos');
    });
  });

  // --- View-mode toggle tests ---

  it('setViewType_toTimeline_setsViewTypeAndCallsGetTimeline', () => {
    const mockTimelinePage: PaginatedData<TimelineGroup> = {
      items: [{ localDate: '2024-05-10', label: 'May 10, 2024', assets: [mockAssets[0]] }],
      pageIndex: 0, totalPages: 1, totalItems: 1,
    };
    const getTimeline = cy.stub().returns(of(mockTimelinePage));

    mountGallery({ getTimeline }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.setViewType('timeline');
      expect(component.viewType).to.equal('timeline');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.wrap(getTimeline).should('have.been.calledWith', '/photos', 0);
  });

  it('setViewType_toGrid_callsGetAssetsAndShowsThumbnailGrid', () => {
    const getAssets = cy.stub().returns(of({ items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2 }));
    const getTimeline = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 }));

    mountGallery({ getAssets, getTimeline }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.setViewType('timeline');
      component.setViewType('grid');
      expect(component.viewType).to.equal('grid');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.wrap(getAssets).should('have.been.called');
  });

  it('setViewType_toTimeline_rendersTimelineViewComponent', () => {
    const mockTimelinePage: PaginatedData<TimelineGroup> = {
      items: [{ localDate: '2024-05-10', label: 'May 10, 2024', assets: [mockAssets[0]] }],
      pageIndex: 0, totalPages: 1, totalItems: 1,
    };
    const getTimeline = cy.stub().returns(of(mockTimelinePage));

    mountGallery({ getTimeline }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.setViewType('timeline');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.get('app-timeline-view').should('exist');
  });

  it('filterChange_inTimelineMode_resetsTimelineAndReloads', () => {
    const getTimeline = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 }));

    mountGallery({ getTimeline }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.setViewType('timeline');
      return Promise.resolve().then(() => {
        component.timelineGroups = [{ localDate: '2024-05-10', label: 'May 10, 2024', assets: [] }];
        component.searchTerm = 'beach';
        component.loadAssets();
        expect(component.timelineGroups).to.deep.equal([]);
        expect(component.timelinePageIndex).to.equal(0);
      });
    });
  });

  it('moveSelectedAssets_moveConfirmed_showsMovedSnackbarAndClearsSelection', () => {
    const getAssets = cy.stub().returns(of(emptyPage));
    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      cy.stub(component['dialog'], 'open').returns({ afterClosed: () => of({ destinationFolder: '/target' }) } as never);
      component.currentFolder = '/photos';
      component.selectedAssets.add(1);
      component.selectedAssets.add(2);
      component.moveSelectedAssets('move');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.contains('Moved 2 asset(s)').should('be.visible');
    cy.wrap(getAssets).should('have.been.called');
  });

  it('moveSelectedAssets_copyConfirmed_showsCopiedSnackbar', () => {
    mountGallery().then(({ fixture }) => {
      const component = fixture.componentInstance;
      cy.stub(component['dialog'], 'open').returns({ afterClosed: () => of({ destinationFolder: '/target' }) } as never);
      component.currentFolder = '/photos';
      component.selectedAssets.add(1);
      component.moveSelectedAssets('copy');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.contains('Copied 1 asset(s)').should('be.visible');
  });

  it('moveSelectedAssets_moveFails_showsErrorSnackbar', () => {
    const moveAssets = cy.stub().returns(throwError(() => new Error('Network error')));
    mountGallery({ moveAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      cy.stub(component['dialog'], 'open').returns({ afterClosed: () => of({ destinationFolder: '/target' }) } as never);
      component.currentFolder = '/photos';
      component.selectedAssets.add(1);
      component.moveSelectedAssets('move');
      return Promise.resolve().then(() => fixture.detectChanges());
    });

    cy.contains('Failed to move assets').should('be.visible');
  });

  it('matSidenavContent_onMount_hasDisplayFlex', () => {
    mountGallery();
    cy.get('mat-sidenav-content').should('have.css', 'display', 'flex');
  });

  it('matSidenavContent_onMount_hasFlexDirectionColumn', () => {
    mountGallery();
    cy.get('mat-sidenav-content').should('have.css', 'flex-direction', 'column');
  });

  it('assetVirtualViewport_folderSelected_hasPositiveHeight', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));
    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
      return Promise.resolve().then(() => fixture.detectChanges());
    });
    cy.get('cdk-virtual-scroll-viewport').invoke('outerHeight').should('be.gt', 0);
  });

  it('assetListRows_folderSelected_areVisible', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));
    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
      return Promise.resolve().then(() => fixture.detectChanges());
    });
    cy.get('.asset-list-row').should('be.visible');
  });

  it('ngOnInit_withoutFolderQueryParam_doesNotPreSelectFolder', () => {
    const getAssets = cy.stub().returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 }));

    const activatedRouteStub = {
      snapshot: {
        queryParamMap: {
          get: (_key: string) => null,
        },
      },
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: { getAssets, catalogAssets: cy.stub(), deleteAssets: cy.stub().returns(of(undefined)), moveAssets: cy.stub().returns(of(true)) } },
        { provide: TagService, useValue: { searchTags: cy.stub().returns(of([])) } },
        { provide: FolderService, useValue: { getFolders: cy.stub().returns(of([])) } },
        { provide: AlbumService, useValue: { getAlbums: cy.stub().returns(of([])) } },
        { provide: SearchPresetService, useValue: { listPresets: cy.stub().returns(of([])), createPreset: cy.stub().returns(of({})), deletePreset: cy.stub().returns(of(undefined)) } },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: AudioPlayerService, useValue: { currentTrack: signal(null), isPlaying: signal(false), play: cy.stub(), loadFolder: cy.stub(), loadPlaylist: cy.stub() } },
      ],
    }).then(({ fixture }) => {
      fixture.detectChanges();
      cy.wrap(fixture.componentInstance).should('have.property', 'currentFolder', '');
    });

    cy.wrap(getAssets).should('not.have.been.called');
  });
});
