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
  it('renders_withExistingFilter_populatesSearchAndRating', () => {
    mountDialog({ search: 'sunset', minRating: 3 });
    cy.get('input[matInput]').first().should('have.value', 'sunset');
    cy.contains('3★ +').should('be.visible');
  });

  it('renders_emptyFilter_showsAnyRating', () => {
    mountDialog({});
    cy.contains('Any rating').should('be.visible');
  });

  it('confirm_searchTyped_closesWithSearchField', () => {
    mountDialog({});
    cy.get('input[matInput]').first().type('beach');
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', { search: 'beach' });
  });

  it('confirm_starClicked_closesWithMinRating', () => {
    mountDialog({});
    cy.get('.filter-star').eq(2).click();
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', { minRating: 3 });
  });

  it('confirm_sameStarClickedTwice_togglesRatingOff', () => {
    mountDialog({ minRating: 3 });
    cy.get('.filter-star').eq(2).click();
    cy.contains('button', 'Save').click();
    cy.get('@dialogClose').should('have.been.calledWith', {});
  });

  it('cancel_clickCancel_closesDialogWithNull', () => {
    mountDialog({ search: 'sunset' });
    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.calledWith', null);
  });
});
