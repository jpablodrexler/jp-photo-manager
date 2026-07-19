import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EditAlbumFilterDialogComponent } from './edit-album-filter-dialog.component';
import { AlbumFilterJson } from '../../../core/models/album.model';

const defaultDialogRef = (): Partial<MatDialogRef<EditAlbumFilterDialogComponent, AlbumFilterJson | null>> => ({
  close: cy.stub().as('dialogClose'),
});

const mountDialog = (filterJson: AlbumFilterJson, dialogRef = defaultDialogRef()) => {
  return cy.mount(EditAlbumFilterDialogComponent, {
    providers: [
      provideNoopAnimations(),
      { provide: MatDialogRef, useValue: dialogRef },
      { provide: MAT_DIALOG_DATA, useValue: { filterJson } },
    ],
  });
};

describe('EditAlbumFilterDialogComponent', () => {
  it('should populate the search field and rating from an existing filter', () => {
    mountDialog({ search: 'sunset', minRating: 3 });
    cy.get('input[matInput]').first().should('have.value', 'sunset');
    cy.contains('3★ +').should('be.visible');
  });

  it('should show "Any rating" for an empty filter', () => {
    mountDialog({});
    cy.contains('Any rating').should('be.visible');
  });

  it('should close the dialog with the search field when Save is clicked after typing', () => {
    mountDialog({});
    cy.get('input[matInput]').first().type('beach');
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', { search: 'beach' });
  });

  it('should close the dialog with the selected minRating when a star is clicked', () => {
    mountDialog({});
    cy.get('.filter-star').eq(2).click();
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', { minRating: 3 });
  });

  it('should toggle the rating off when the same star is clicked twice', () => {
    mountDialog({ minRating: 3 });
    cy.get('.filter-star').eq(2).click();
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', {});
  });

  it('should close the dialog with null when Cancel is clicked', () => {
    mountDialog({ search: 'sunset' });
    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.calledWith', null);
  });
});
