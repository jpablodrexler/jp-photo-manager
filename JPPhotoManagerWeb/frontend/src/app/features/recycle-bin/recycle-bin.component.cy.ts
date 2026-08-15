import { of, throwError } from 'rxjs';
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
      rating: 0, tags: [], fileType: 'IMAGE', isVideo: false,
    },
    {
      assetId: 2, folderId: 1, folderPath: '/photos', fileName: 'beach.jpg',
      fileSize: 512000, thumbnailCreationDateTime: '2024-06-02T10:00:00',
      hash: 'def456', thumbnailUrl: '/api/assets/2/thumbnail', imageUrl: '/api/assets/2/image',
      rating: 0, tags: [], fileType: 'IMAGE', isVideo: false,
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

  it('should render a thumbnail card for each deleted asset on the page', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    const getRecycleBin = cy.stub().returns(of(page));

    mountRecycleBin({ getRecycleBin });

    cy.get('app-thumbnail').should('have.length', 2);
  });

  it('should restore the selected asset and show a confirmation snackbar', () => {
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

  it('should permanently delete the selected asset and show a confirmation snackbar', () => {
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

  it('should call purgeAll when the empty recycle bin button is clicked', () => {
    const purgeAll = cy.stub().returns(of(undefined));

    mountRecycleBin({ purgeAll });

    cy.get('[title="Empty Recycle Bin"]').click();
    cy.wrap(purgeAll).should('have.been.called');
  });

  it('should show the empty state when the recycle bin has no assets', () => {
    mountRecycleBin();
    cy.contains('.empty-state', 'Recycle bin is empty.').should('exist');
  });

  it('should show a snackbar when loading the recycle bin fails', () => {
    mountRecycleBin({ getRecycleBin: cy.stub().returns(throwError(() => new Error('load failed'))) });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load recycle bin');
  });

  it('should not show the restore/delete actions when nothing is selected', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    mountRecycleBin({ getRecycleBin: cy.stub().returns(of(page)) });
    cy.get('button').contains('Restore').should('not.exist');
    cy.get('button').contains('Delete Permanently').should('not.exist');
  });

  it('should mark a thumbnail as selected when its cell is clicked, and unselect on a second click', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    mountRecycleBin({ getRecycleBin: cy.stub().returns(of(page)) });

    cy.get('.asset-cell').first().click();
    cy.get('.asset-cell').first().find('.thumbnail-card').should('have.class', 'selected');
    cy.get('button').contains('Restore').should('exist');

    cy.get('.asset-cell').first().click();
    cy.get('.asset-cell').first().find('.thumbnail-card').should('not.have.class', 'selected');
    cy.get('button').contains('Restore').should('not.exist');
  });

  it('should show a snackbar when restoring assets fails', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    mountRecycleBin({
      getRecycleBin: cy.stub().returns(of(page)),
      restoreAssets: cy.stub().returns(throwError(() => new Error('restore failed'))),
    }).then(({ fixture }) => {
      fixture.componentInstance.toggleSelection(deletedAssets[0]);
      fixture.detectChanges();
    });
    cy.get('button').contains('Restore').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to restore assets');
  });

  it('should show a snackbar when purging selected assets fails', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    mountRecycleBin({
      getRecycleBin: cy.stub().returns(of(page)),
      purgeAssets: cy.stub().returns(throwError(() => new Error('purge failed'))),
    }).then(({ fixture }) => {
      fixture.componentInstance.toggleSelection(deletedAssets[0]);
      fixture.detectChanges();
    });
    cy.get('button').contains('Delete Permanently').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to delete assets');
  });

  it('should show a confirmation snackbar when purgeAll succeeds', () => {
    mountRecycleBin();
    cy.get('[title="Empty Recycle Bin"]').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Recycle bin emptied');
  });

  it('should show a snackbar when purgeAll fails', () => {
    mountRecycleBin({ purgeAll: cy.stub().returns(throwError(() => new Error('purge-all failed'))) });
    cy.get('[title="Empty Recycle Bin"]').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to empty recycle bin');
  });

  it('should clear the selection after a successful restore', () => {
    const page: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 1, totalItems: 2 };
    mountRecycleBin({ getRecycleBin: cy.stub().returns(of(page)) }).then(({ fixture }) => {
      fixture.componentInstance.toggleSelection(deletedAssets[0]);
      fixture.detectChanges();
    });
    cy.get('button').contains('Restore').click();
    cy.get('button').contains('Restore').should('not.exist');
  });

  describe('pagination', () => {
    const page0: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 0, totalPages: 3, totalItems: 8 };

    it('should not render pagination controls when there is only one page', () => {
      mountRecycleBin();
      cy.get('.pagination-bar').should('not.exist');
    });

    it('should render pagination controls and disable "previous" on the first page', () => {
      mountRecycleBin({ getRecycleBin: cy.stub().returns(of(page0)) });
      cy.contains('.pagination-bar span', 'Page 1 of 3').should('exist');
      cy.get('.pagination-bar button').first().should('be.disabled');
      cy.get('.pagination-bar button').last().should('not.be.disabled');
    });

    it('should load the next page when the next button is clicked', () => {
      mountRecycleBin({ getRecycleBin: cy.stub().returns(of(page0)) }).then(({ serviceStub }) => {
        cy.get('.pagination-bar button').last().click();
        cy.wrap(serviceStub.getRecycleBin).should('have.been.calledWith', 1);
      });
    });

    it('should disable "next" on the last page', () => {
      const lastPage: PaginatedData<Asset> = { items: deletedAssets, pageIndex: 2, totalPages: 3, totalItems: 8 };
      mountRecycleBin({ getRecycleBin: cy.stub().returns(of(lastPage)) });
      cy.get('.pagination-bar button').last().should('be.disabled');
      cy.get('.pagination-bar button').first().should('not.be.disabled');
    });
  });
});
