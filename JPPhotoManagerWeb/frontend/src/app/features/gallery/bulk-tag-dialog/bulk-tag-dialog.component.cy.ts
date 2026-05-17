import { mount } from 'cypress/angular';
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
  it('renders_withAssetIds_showsCorrectTitle', () => {
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

  it('cancel_clickCancel_closesDialogWithFalse', () => {
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

  it('confirm_withTagsToAdd_callsBulkAddAndCloses', () => {
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

  it('confirm_withTagsToRemove_callsBulkRemoveAndCloses', () => {
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

  it('confirm_noTags_closesWithFalse', () => {
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
