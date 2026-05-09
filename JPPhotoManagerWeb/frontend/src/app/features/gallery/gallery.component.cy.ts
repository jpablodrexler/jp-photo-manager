import { mount } from 'cypress/angular';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { GalleryComponent } from './gallery.component';
import { AssetService } from '../../core/services/asset.service';
import { AlbumService } from '../../core/services/album.service';
import { FolderService } from '../../core/services/folder.service';
import { Asset } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';

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
    },
  ];

  const emptyPage: PaginatedData<Asset> = { items: [], pageIndex: 0, totalPages: 0, totalItems: 0 };

  function mountGallery(assetServiceOverrides: Partial<AssetService> = {}) {
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of(emptyPage)),
      catalogAssets: cy.stub(),
      deleteAssets: cy.stub().returns(of(undefined)),
      moveAssets: cy.stub().returns(of(true)),
      ...assetServiceOverrides,
    };

    const folderServiceStub: Partial<FolderService> = {
      getFolders: cy.stub().returns(of([])),
    };

    const albumServiceStub: Partial<AlbumService> = {
      getAlbums: cy.stub().returns(of([])),
    };

    return cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
        { provide: FolderService, useValue: folderServiceStub },
        { provide: AlbumService, useValue: albumServiceStub },
      ],
    }).then(result => ({ ...result, assetServiceStub }));
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

    cy.get('app-thumbnail').should('have.length', 2);
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
    cy.get('app-thumbnail').should('have.length', 2);
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
});
