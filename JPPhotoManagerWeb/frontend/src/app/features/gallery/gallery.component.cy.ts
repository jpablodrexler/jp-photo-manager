import { mount } from 'cypress/angular';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { GalleryComponent } from './gallery.component';
import { AssetService } from '../../core/services/asset.service';
import { Asset } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';
import { MockEventSource } from '../../../../cypress/support/mock-event-source';

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
    const mockSource = new MockEventSource();
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of(emptyPage)),
      catalogAssets: cy.stub().returns(mockSource),
      deleteAssets: cy.stub().returns(of(undefined)),
      moveAssets: cy.stub().returns(of(true)),
      ...assetServiceOverrides,
    };

    return cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    }).then(result => ({ ...result, mockSource, assetServiceStub }));
  }

  it('should create the component', () => {
    mountGallery();
    cy.get('app-gallery').should('exist');
  });

  it('should start in thumbnails view mode', () => {
    mountGallery().then(({ fixture }) => {
      expect(fixture.componentInstance.viewMode).to.equal('thumbnails');
    });
  });

  it('should start cataloging on init', () => {
    const catalogAssets = cy.stub().returns(new MockEventSource());
    mountGallery({ catalogAssets });
    cy.wrap(catalogAssets).should('have.been.calledOnce');
  });

  it('should load assets when a folder is selected', () => {
    const getAssets = cy.stub().returns(of({
      items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 2,
    }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      fixture.componentInstance.onFolderSelected('/photos');
      fixture.detectChanges();
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

  it('should go to next page and reload assets', () => {
    const getAssets = cy.stub().returns(of({ items: mockAssets, pageIndex: 0, totalPages: 3, totalItems: 6 }));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.totalPages = 3;
      component.pageIndex = 0;
      fixture.detectChanges();

      component.nextPage();
      expect(component.pageIndex).to.equal(1);
    });
  });

  it('should go to previous page and reload assets', () => {
    const getAssets = cy.stub().returns(of(emptyPage));

    mountGallery({ getAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.currentFolder = '/photos';
      component.pageIndex = 2;
      component.prevPage();
      expect(component.pageIndex).to.equal(1);
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

  it('should update catalogProgress when catalog SSE event is received', () => {
    const mockSource = new MockEventSource();
    const catalogAssets = cy.stub().returns(mockSource);

    mountGallery({ catalogAssets }).then(({ fixture }) => {
      mockSource.emit('catalog', { percentCompleted: 60, message: 'Scanning…', reason: 'SCANNING' });
      fixture.detectChanges();
      expect(fixture.componentInstance.catalogProgress).to.equal(60);
    });
  });

  it('should stop cataloging when SSE error is received', () => {
    const mockSource = new MockEventSource();
    const catalogAssets = cy.stub().returns(mockSource);

    mountGallery({ catalogAssets }).then(({ fixture }) => {
      expect(fixture.componentInstance.cataloging).to.be.true;
      mockSource.dispatchEvent(new Event('error'));
      fixture.detectChanges();
      expect(fixture.componentInstance.cataloging).to.be.false;
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
});
