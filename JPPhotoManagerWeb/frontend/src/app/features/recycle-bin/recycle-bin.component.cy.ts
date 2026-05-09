import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { RecycleBinComponent } from './recycle-bin.component';
import { RecycleBinService } from '../../core/services/recycle-bin.service';
import { Asset } from '../../core/models/asset.model';
import { PaginatedData } from '../../core/models/paginated-data.model';

describe('RecycleBinComponent', () => {
  const deletedAssets: Asset[] = [
    {
      assetId: 1, folderId: 1, folderPath: '/photos', fileName: 'sunset.jpg',
      fileSize: 1024000, thumbnailCreationDateTime: '2024-06-01T10:00:00',
      hash: 'abc123', thumbnailUrl: '/api/assets/1/thumbnail', imageUrl: '/api/assets/1/image',
    },
    {
      assetId: 2, folderId: 1, folderPath: '/photos', fileName: 'beach.jpg',
      fileSize: 512000, thumbnailCreationDateTime: '2024-06-02T10:00:00',
      hash: 'def456', thumbnailUrl: '/api/assets/2/thumbnail', imageUrl: '/api/assets/2/image',
    },
  ];

  const emptyPage: PaginatedData<Asset> = { items: [], pageIndex: 0, totalPages: 0, totalItems: 0 };

  function mountRecycleBin(overrides: Partial<RecycleBinService> = {}) {
    const serviceStub: Partial<RecycleBinService> = {
      getRecycleBin: cy.stub().returns(of(emptyPage)),
      restoreAssets: cy.stub().returns(of(undefined)),
      purgeAssets: cy.stub().returns(of(undefined)),
      purgeAll: cy.stub().returns(of(undefined)),
      ...overrides,
    };

    return cy.mount(RecycleBinComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: RecycleBinService, useValue: serviceStub },
      ],
    }).then(result => ({ ...result, serviceStub }));
  }

  it('loadPage_withDeletedAssets_rendersTwoThumbnailCards', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    const getRecycleBin = cy.stub().returns(of(page));

    mountRecycleBin({ getRecycleBin });

    cy.get('app-thumbnail').should('have.length', 2);
  });

  it('restoreSelected_withOneSelectedAsset_callsRestoreAssetsAndShowsSnackBar', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    const getRecycleBin = cy.stub().returns(of(page));
    const restoreAssets = cy.stub().returns(of(undefined));

    mountRecycleBin({ getRecycleBin, restoreAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.toggleSelection(deletedAssets[0]);
      fixture.detectChanges();
    });

    cy.get('button').contains('Restore').click();
    cy.wrap(restoreAssets).should('have.been.calledWith', [1]);
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Restored successfully');
  });

  it('purgeSelected_withOneSelectedAsset_callsPurgeAssetsAndShowsSnackBar', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    const getRecycleBin = cy.stub().returns(of(page));
    const purgeAssets = cy.stub().returns(of(undefined));

    mountRecycleBin({ getRecycleBin, purgeAssets }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.toggleSelection(deletedAssets[0]);
      fixture.detectChanges();
    });

    cy.get('button').contains('Delete Permanently').click();
    cy.wrap(purgeAssets).should('have.been.calledWith', [1]);
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Permanently deleted');
  });

  it('purgeAll_clicksEmptyButton_callsPurgeAll', () => {
    const purgeAll = cy.stub().returns(of(undefined));

    mountRecycleBin({ purgeAll });

    cy.get('[title="Empty Recycle Bin"]').click();
    cy.wrap(purgeAll).should('have.been.called');
  });
});
