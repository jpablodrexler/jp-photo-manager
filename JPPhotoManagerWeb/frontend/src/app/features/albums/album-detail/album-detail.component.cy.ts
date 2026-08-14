import { NEVER, of, throwError } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { AlbumDetailComponent } from './album-detail.component';
import { AlbumService } from '../../../core/services/album.service';
import { Album, AlbumFilterJson } from '../../../core/models/album.model';
import { Asset } from '../../../core/models/asset.model';

describe('AlbumDetailComponent', () => {
  const ALBUM_ID = 5;

  const mockAssets: Asset[] = [
    { assetId: 101, folderId: 1, folderPath: '/photos', fileName: 'a.jpg', fileSize: 1000, thumbnailCreationDateTime: '2024-01-01T00:00:00', hash: 'h1', thumbnailUrl: '/api/assets/101/thumbnail', imageUrl: '/api/assets/101/image', rating: 0, tags: [], fileType: 'IMAGE', isVideo: false },
    { assetId: 102, folderId: 1, folderPath: '/photos', fileName: 'b.jpg', fileSize: 2000, thumbnailCreationDateTime: '2024-01-01T00:00:00', hash: 'h2', thumbnailUrl: '/api/assets/102/thumbnail', imageUrl: '/api/assets/102/image', rating: 0, tags: [], fileType: 'IMAGE', isVideo: false },
    { assetId: 103, folderId: 1, folderPath: '/photos', fileName: 'c.jpg', fileSize: 3000, thumbnailCreationDateTime: '2024-01-01T00:00:00', hash: 'h3', thumbnailUrl: '/api/assets/103/thumbnail', imageUrl: '/api/assets/103/image', rating: 0, tags: [], fileType: 'IMAGE', isVideo: false }
  ];

  const mockAlbum: Album = {
    albumId: ALBUM_ID,
    name: 'My Album',
    description: null,
    createdAt: '2024-01-01T00:00:00Z',
    assets: { items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 3 }
  };

  const smartAlbum: Album = {
    albumId: ALBUM_ID,
    name: 'Smart Album',
    description: null,
    createdAt: '2024-01-01T00:00:00Z',
    assets: { items: mockAssets, pageIndex: 0, totalPages: 1, totalItems: 3 },
    filterJson: { minRating: 4 }
  };

  function mountDetail(album: Album = mockAlbum, albumServiceOverrides: Partial<AlbumService> = {}) {
    const serviceStub: Partial<AlbumService> = {
      getAlbum: cy.stub().returns(of(album)),
      removeAssets: cy.stub().returns(of(undefined)),
      updateAlbum: cy.stub().returns(of({ albumId: ALBUM_ID, name: album.name, description: null, assetCount: 3, createdAt: '2024-01-01T00:00:00Z' })),
      ...albumServiceOverrides
    };
    return cy.mount(AlbumDetailComponent, {
      providers: [
        { provide: AlbumService, useValue: serviceStub },
        provideNoopAnimations(),
        // provideRouter(...) supplies its own root ActivatedRoute provider;
        // ours must come after it in this array to win (Angular resolves a
        // non-multi token to the *last* matching provider), or
        // route.snapshot.paramMap.get('id') silently returns null and
        // albumId falls back to Number(null) === 0 instead of ALBUM_ID.
        provideRouter([{ path: '**', component: AlbumDetailComponent }]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_key: string) => String(ALBUM_ID) } } }
        },
      ]
    }).then(({ component, fixture }) => ({ component, fixture, serviceStub }));
  }

  it('should render a thumbnail for each asset loaded on init', () => {
    mountDetail();
    cy.get('app-thumbnail').should('have.length', 3);
    cy.get('mat-toolbar').contains('My Album').should('exist');
  });

  it('should call the service removeAssets method when removing an asset', () => {
    mountDetail().then(({ component, serviceStub }) => {
      component.albumId = ALBUM_ID;
      component.removeAsset(mockAssets[0].assetId);
      cy.wrap(serviceStub.removeAssets).should('have.been.calledWith', ALBUM_ID, [mockAssets[0].assetId]);
    });
  });

  it('should show the smart album banner and hide the remove button for a smart album', () => {
    mountDetail(smartAlbum);
    cy.get('.smart-album-banner').should('exist').and('contain.text', 'Smart album');
    cy.get('.remove-btn').should('not.exist');
  });

  it('should show the remove button and no banner for a static album', () => {
    mountDetail(mockAlbum);
    cy.get('.smart-album-banner').should('not.exist');
    cy.get('.remove-btn').should('exist');
  });

  it('should summarize every set filter criterion in the smart album banner', () => {
    const fullFilterAlbum: Album = {
      ...smartAlbum,
      filterJson: { search: 'sunset', dateFrom: '2024-01-01', dateTo: '2024-12-31', minRating: 4 }
    };
    mountDetail(fullFilterAlbum);
    cy.get('.smart-album-banner')
      .should('contain.text', 'Search: sunset')
      .and('contain.text', 'From: 2024-01-01')
      .and('contain.text', 'To: 2024-12-31')
      .and('contain.text', 'Min rating: 4');
  });

  it('should fall back to "No criteria" when the smart album filter has no fields set', () => {
    const emptyFilterAlbum: Album = { ...smartAlbum, filterJson: {} };
    mountDetail(emptyFilterAlbum);
    cy.get('.smart-album-banner').should('contain.text', 'No criteria');
  });

  it('should default totalPages to 0 before the album has loaded', () => {
    mountDetail(mockAlbum, { getAlbum: cy.stub().returns(NEVER) }).then(({ component }) => {
      expect(component.totalPages).to.equal(0);
    });
  });

  it('should call updateAlbum with the new filter when editing a smart album filter', () => {
    const updatedFilter: AlbumFilterJson = { minRating: 4 };
    const dialogRef = { afterClosed: () => of(updatedFilter) };
    mountDetail(smartAlbum).then(({ component, serviceStub }) => {
      component.albumId = ALBUM_ID;
      component.album.set(smartAlbum);
      (component as unknown as { dialog: MatDialog }).dialog = { open: () => dialogRef } as unknown as MatDialog;
      component.openEditFilterDialog();
      cy.wrap(serviceStub.updateAlbum).should('have.been.calledWith', ALBUM_ID, {
        name: 'Smart Album',
        filterJson: updatedFilter
      });
    });
  });

  it('should show a snackbar and navigate back to /albums when loading the album fails', () => {
    // getAlbum() rejects synchronously (throwError emits on subscribe), so
    // the error branch's router.navigate() call happens during ngOnInit —
    // before mount even resolves. A real Router must be stubbed *before*
    // mounting, not patched afterwards, or the call is missed entirely.
    const navigateStub = cy.stub();
    cy.mount(AlbumDetailComponent, {
      providers: [
        { provide: AlbumService, useValue: { getAlbum: cy.stub().returns(throwError(() => new Error('load failed'))) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (_key: string) => String(ALBUM_ID) } } } },
        { provide: Router, useValue: { navigate: navigateStub } },
        provideNoopAnimations(),
      ]
    });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load album');
    cy.wrap(navigateStub).should('have.been.calledWith', ['/albums']);
  });

  it('should show the empty state when the album has no photos', () => {
    const emptyAlbum: Album = { ...mockAlbum, assets: { items: [], pageIndex: 0, totalPages: 1, totalItems: 0 } };
    mountDetail(emptyAlbum);
    cy.contains('.empty-state', 'No photos in this album.').should('exist');
  });

  it('should remove an asset from the DOM when the remove button is clicked', () => {
    mountDetail().then(({ serviceStub }) => {
      cy.get('.remove-btn').first().click();
      cy.wrap(serviceStub.removeAssets).should('have.been.calledWith', ALBUM_ID, [mockAssets[0].assetId]);
      cy.get('.mat-mdc-snack-bar-label').should('contain', 'Removed from album');
    });
  });

  it('should show a snackbar when removing an asset fails', () => {
    mountDetail(mockAlbum, { removeAssets: cy.stub().returns(throwError(() => new Error('remove failed'))) });
    cy.get('.remove-btn').first().click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to remove asset');
  });

  describe('pagination', () => {
    const page0: Album = {
      ...mockAlbum,
      assets: { items: mockAssets, pageIndex: 0, totalPages: 3, totalItems: 9 }
    };

    it('should not render pagination controls when there is only one page', () => {
      mountDetail(mockAlbum);
      cy.get('.pagination').should('not.exist');
    });

    it('should render pagination controls and disable "previous" on the first page', () => {
      mountDetail(page0);
      cy.get('.pagination').should('exist');
      cy.contains('.pagination span', '1 / 3').should('exist');
      cy.get('.pagination button').first().should('be.disabled');
      cy.get('.pagination button').last().should('not.be.disabled');
    });

    it('should load the next page when the next button is clicked', () => {
      mountDetail(page0).then(({ serviceStub }) => {
        cy.get('.pagination button').last().click();
        cy.wrap(serviceStub.getAlbum).should('have.been.calledWith', ALBUM_ID, 1);
      });
    });

    it('should disable "next" on the last page', () => {
      const lastPage: Album = {
        ...mockAlbum,
        assets: { items: mockAssets, pageIndex: 2, totalPages: 3, totalItems: 9 }
      };
      mountDetail(lastPage).then(({ component, fixture }) => {
        // ngOnInit's own loadPage(0) subscription can still be in flight
        // when this .then() callback runs, and it would silently
        // overwrite a currentPage override applied too early. Wait for
        // the pagination bar (driven by totalPages, unaffected by which
        // page is "current") to confirm the initial load has settled
        // before overriding currentPage.
        cy.get('.pagination').should('exist').then(() => {
          // A signal write from Cypress's callback context happens
          // outside Angular's zone, so it won't auto-trigger change
          // detection in this zone.js app — force it explicitly.
          component.currentPage.set(2);
          fixture.detectChanges();
        });
      });
      cy.get('.pagination button').last().should('be.disabled');
    });

    it('should load the previous page when the previous button is clicked', () => {
      const secondPage: Album = {
        ...mockAlbum,
        assets: { items: mockAssets, pageIndex: 1, totalPages: 3, totalItems: 9 }
      };
      mountDetail(secondPage, { getAlbum: cy.stub().returns(of(secondPage)) }).then(({ component, fixture, serviceStub }) => {
        cy.get('.pagination').should('exist').then(() => {
          component.currentPage.set(1);
          fixture.detectChanges();
        });
        cy.get('.pagination button').first().should('not.be.disabled').click();
        // getAlbum is called once by ngOnInit's own loadPage(0) and again by
        // the previous-button click's loadPage(0) — assert the call count
        // increased so this actually verifies the click is wired, not just
        // that page 0 was requested at some point.
        cy.wrap(serviceStub.getAlbum).should('have.callCount', 2);
      });
    });
  });

  it('should open the edit filter dialog and update the filter through the UI', () => {
    const updatedFilter: AlbumFilterJson = { search: 'sunset' };
    const dialogOpenStub = cy.stub().returns({ afterClosed: () => of(updatedFilter) });
    const updateAlbum = cy.stub().returns(of({ albumId: ALBUM_ID, name: smartAlbum.name, description: null, assetCount: 3, createdAt: '2024-01-01T00:00:00Z' }));

    cy.mount(AlbumDetailComponent, {
      providers: [
        { provide: AlbumService, useValue: { getAlbum: cy.stub().returns(of(smartAlbum)), updateAlbum } },
        { provide: MatDialog, useValue: { open: dialogOpenStub } },
        provideNoopAnimations(),
        // provideRouter(...) must come before the ActivatedRoute override
        // below, or its own root ActivatedRoute wins and albumId falls
        // back to 0 — see the comment in mountDetail() above.
        provideRouter([{ path: '**', component: AlbumDetailComponent }]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (_key: string) => String(ALBUM_ID) } } } },
      ]
    });

    cy.get('button').contains('Edit filter').click();
    cy.wrap(dialogOpenStub).should('have.been.called');
    cy.wrap(updateAlbum).should('have.been.calledWith', ALBUM_ID, { name: 'Smart Album', filterJson: updatedFilter });
  });

  it('should not call updateAlbum when the edit filter dialog is dismissed without a result', () => {
    const dialogOpenStub = cy.stub().returns({ afterClosed: () => of(undefined) });
    const updateAlbum = cy.stub().returns(of({ albumId: ALBUM_ID, name: smartAlbum.name, description: null, assetCount: 3, createdAt: '2024-01-01T00:00:00Z' }));

    cy.mount(AlbumDetailComponent, {
      providers: [
        { provide: AlbumService, useValue: { getAlbum: cy.stub().returns(of(smartAlbum)), updateAlbum } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (_key: string) => String(ALBUM_ID) } } } },
        { provide: MatDialog, useValue: { open: dialogOpenStub } },
        provideNoopAnimations(),
        provideRouter([{ path: '**', component: AlbumDetailComponent }])
      ]
    });

    cy.get('button').contains('Edit filter').click();
    cy.wrap(dialogOpenStub).should('have.been.called');
    cy.wrap(updateAlbum).should('not.have.been.called');
  });

  it('should show a snackbar when updating the filter fails', () => {
    const dialogRef = { afterClosed: () => of({ minRating: 5 } as AlbumFilterJson) };
    mountDetail(smartAlbum, { updateAlbum: cy.stub().returns(throwError(() => new Error('update failed'))) }).then(({ component }) => {
      component.albumId = ALBUM_ID;
      component.album.set(smartAlbum);
      (component as unknown as { dialog: MatDialog }).dialog = { open: () => dialogRef } as unknown as MatDialog;
      component.openEditFilterDialog();
    });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to update filter');
  });

  it('should do nothing when opening the edit filter dialog with no album loaded', () => {
    mountDetail(mockAlbum, { getAlbum: cy.stub().returns(of(mockAlbum)) }).then(({ component }) => {
      component.album.set(null);
      const openStub = cy.stub();
      (component as unknown as { dialog: MatDialog }).dialog = { open: openStub } as unknown as MatDialog;
      component.openEditFilterDialog();
      expect(openStub).to.not.have.been.called;
    });
  });
});
