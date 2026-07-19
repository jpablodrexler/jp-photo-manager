import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AddToAlbumDialogComponent } from './add-to-album-dialog.component';
import { AlbumSummary } from '../../../core/models/album.model';

const staticAlbum: AlbumSummary = {
  albumId: 1, name: 'Vacation', description: null, assetCount: 12, createdAt: '2024-01-01T00:00:00Z'
};
const smartAlbum: AlbumSummary = {
  albumId: 2, name: 'Top Rated', description: null, assetCount: 5, createdAt: '2024-01-02T00:00:00Z',
  filterJson: { minRating: 4 }
};

const defaultDialogRef = (): Partial<MatDialogRef<AddToAlbumDialogComponent>> => ({
  close: cy.stub().as('dialogClose'),
});

const mountDialog = (albums: AlbumSummary[], dialogRef = defaultDialogRef()) => {
  return cy.mount(AddToAlbumDialogComponent, {
    providers: [
      provideNoopAnimations(),
      { provide: MatDialogRef, useValue: dialogRef },
      { provide: MAT_DIALOG_DATA, useValue: { albums } },
    ],
  });
};

describe('AddToAlbumDialogComponent', () => {
  it('should list each album with its asset count', () => {
    mountDialog([staticAlbum, smartAlbum]);
    cy.contains('Vacation (12 photos)').should('be.visible');
    cy.contains('Top Rated (5 photos)').should('be.visible');
  });

  it('should show only the create-new option when there are no albums', () => {
    mountDialog([]);
    cy.contains('Select an existing album').should('not.exist');
    cy.contains('Create a new album').should('be.visible');
  });

  it('should disable the radio button for a smart album', () => {
    mountDialog([staticAlbum, smartAlbum]);
    cy.contains('mat-radio-button', 'Top Rated').find('input[type="radio"]').should('be.disabled');
    cy.contains('mat-radio-button', 'Vacation').find('input[type="radio"]').should('not.be.disabled');
  });

  it('should disable the Add button when nothing is selected or typed', () => {
    mountDialog([staticAlbum]);
    cy.contains('button', 'Add').should('be.disabled');
  });

  it('should close the dialog with the albumId when an existing album is selected', () => {
    mountDialog([staticAlbum]);
    cy.contains('mat-radio-button', 'Vacation').find('input[type="radio"]').click({ force: true });
    cy.contains('button', 'Add').should('not.be.disabled').click();
    cy.get('@dialogClose').should('have.been.calledWith', { albumId: 1, newAlbumName: null });
  });

  it('should close the dialog with the trimmed new album name when typed', () => {
    mountDialog([staticAlbum]);
    cy.get('input[matInput]').type('  Fresh Album  ');
    cy.contains('button', 'Add').should('not.be.disabled').click();
    cy.get('@dialogClose').should('have.been.calledWith', { albumId: null, newAlbumName: 'Fresh Album' });
  });

  it('should close the dialog with no args when Cancel is clicked', () => {
    mountDialog([staticAlbum]);
    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.called');
  });
});
