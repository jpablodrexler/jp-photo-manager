import { of } from 'rxjs';
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
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_key: string) => String(ALBUM_ID) } } }
        },
        provideNoopAnimations(),
        provideRouter([{ path: '**', component: AlbumDetailComponent }])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
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

  it('should call updateAlbum with the new filter when editing a smart album filter', () => {
    const updatedFilter: AlbumFilterJson = { minRating: 4 };
    const dialogRef = { afterClosed: () => of(updatedFilter) };
    mountDetail(smartAlbum).then(({ component, serviceStub }) => {
      component.albumId = ALBUM_ID;
      component.album = smartAlbum;
      (component as unknown as { dialog: MatDialog }).dialog = { open: () => dialogRef } as unknown as MatDialog;
      component.openEditFilterDialog();
      cy.wrap(serviceStub.updateAlbum).should('have.been.calledWith', ALBUM_ID, {
        name: 'Smart Album',
        filterJson: updatedFilter
      });
    });
  });
});
