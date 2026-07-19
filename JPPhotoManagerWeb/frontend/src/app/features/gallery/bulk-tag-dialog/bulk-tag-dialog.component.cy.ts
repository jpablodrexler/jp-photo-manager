import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BulkTagDialogComponent } from './bulk-tag-dialog.component';
import { TagService } from '../../../core/services/tag.service';

const defaultTagService = (): Partial<TagService> => ({
  bulkAddTag: cy.stub().returns(of(undefined)),
  bulkRemoveTag: cy.stub().returns(of(undefined)),
  searchTags: cy.stub().returns(of([])),
});

const defaultDialogRef = (): Partial<MatDialogRef<BulkTagDialogComponent>> => ({
  close: cy.stub().as('dialogClose'),
});

describe('BulkTagDialogComponent', () => {
  it('should show the correct title with the asset count', () => {
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1, 2, 3] } },
      ],
    });

    cy.contains('Tag 3 asset(s)').should('be.visible');
  });

  it('should close the dialog with false when Cancel is clicked', () => {
    const dialogRef = defaultDialogRef();
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.contains('Cancel').click();
    cy.get('@dialogClose').should('have.been.calledWith', false);
  });

  it('should call bulkAddTag and close the dialog when tags to add are confirmed', () => {
    const bulkAddStub = cy.stub().returns(of(undefined));
    const tagService = { ...defaultTagService(), bulkAddTag: bulkAddStub } as Partial<TagService>;
    const dialogRef = defaultDialogRef();

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [10, 20] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('vacation{enter}');
    cy.contains('vacation').should('be.visible');
    cy.contains('Apply').click();
    cy.wrap(bulkAddStub).should('have.been.calledWith', [10, 20], 'vacation');
    cy.get('@dialogClose').should('have.been.calledWith', true);
  });

  it('should call bulkRemoveTag and close the dialog when tags to remove are confirmed', () => {
    const bulkRemoveStub = cy.stub().returns(of(undefined));
    const tagService = { ...defaultTagService(), bulkRemoveTag: bulkRemoveStub } as Partial<TagService>;
    const dialogRef = defaultDialogRef();

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [5] } },
      ],
    });

    cy.get('input[placeholder="Remove tag…"]').type('sunset{enter}');
    cy.contains('sunset').should('be.visible');
    cy.contains('Apply').click();
    cy.wrap(bulkRemoveStub).should('have.been.calledWith', [5], 'sunset');
    cy.get('@dialogClose').should('have.been.calledWith', true);
  });

  it('should close the dialog with false when confirmed with no tags', () => {
    const dialogRef = defaultDialogRef();
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.contains('Apply').click();
    cy.get('@dialogClose').should('have.been.calledWith', false);
  });
});
