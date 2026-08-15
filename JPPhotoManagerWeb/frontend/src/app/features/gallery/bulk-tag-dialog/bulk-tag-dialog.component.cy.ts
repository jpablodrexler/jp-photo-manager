import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';
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

  it('should show add-tag suggestions from the autocomplete and select one', () => {
    const searchTagsStub = cy.stub().returns(of(['vacation', 'valley']));
    const tagService = { ...defaultTagService(), searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('va');
    cy.get('mat-option').should('have.length', 2);
    cy.get('mat-option').first().click();
    cy.contains('vacation').should('be.visible');
  });

  it('should show remove-tag suggestions from the autocomplete and select one', () => {
    const searchTagsStub = cy.stub().returns(of(['sunset']));
    const tagService = { ...defaultTagService(), searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Remove tag…"]').type('su');
    cy.get('mat-option').should('have.length', 1);
    cy.get('mat-option').first().click();
    cy.contains('sunset').should('be.visible');
  });

  it('should clear add-tag suggestions when the query is cleared back to empty', () => {
    const searchTagsStub = cy.stub().returns(of(['vacation']));
    const tagService = { ...defaultTagService(), searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('va');
    cy.get('mat-option').should('have.length', 1);
    cy.get('input[placeholder="Add tag…"]').clear();
    cy.get('mat-option').should('not.exist');
  });

  it('should clear remove-tag suggestions when the query is cleared back to empty', () => {
    const searchTagsStub = cy.stub().returns(of(['sunset']));
    const tagService = { ...defaultTagService(), searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Remove tag…"]').type('su');
    cy.get('mat-option').should('have.length', 1);
    cy.get('input[placeholder="Remove tag…"]').clear();
    cy.get('mat-option').should('not.exist');
  });

  it('should remove a chip from the tags-to-add list when its remove button is clicked', () => {
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('vacation{enter}');
    cy.contains('vacation').should('be.visible');
    cy.get('[aria-label="Remove vacation"]').click();
    cy.contains('vacation').should('not.exist');
  });

  it('should remove a chip from the tags-to-remove list when its remove button is clicked', () => {
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Remove tag…"]').type('sunset{enter}');
    cy.contains('sunset').should('be.visible');
    cy.get('[aria-label="Remove sunset"]').click();
    cy.contains('sunset').should('not.exist');
  });

  it('should call both bulkAddTag and bulkRemoveTag when both lists have entries', () => {
    const bulkAddStub = cy.stub().returns(of(undefined));
    const bulkRemoveStub = cy.stub().returns(of(undefined));
    const tagService = {
      ...defaultTagService(),
      bulkAddTag: bulkAddStub,
      bulkRemoveTag: bulkRemoveStub,
    } as Partial<TagService>;
    const dialogRef = defaultDialogRef();

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [7] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('beach{enter}');
    cy.get('input[placeholder="Remove tag…"]').type('old-tag{enter}');
    cy.contains('Apply').click();
    cy.wrap(bulkAddStub).should('have.been.calledWith', [7], 'beach');
    cy.wrap(bulkRemoveStub).should('have.been.calledWith', [7], 'old-tag');
    cy.get('@dialogClose').should('have.been.calledWith', true);
  });

  it('should not add a duplicate tag to the tags-to-add list', () => {
    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: defaultTagService() },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    }).then(({ component }) => {
      cy.get('input[placeholder="Add tag…"]').type('vacation{enter}');
      cy.get('input[placeholder="Add tag…"]').type('vacation{enter}').then(() => {
        expect(component.tagsToAdd).to.deep.equal(['vacation']);
      });
    });
  });

  it('should reset isSaving and keep the dialog open when applying fails', () => {
    const bulkAddStub = cy.stub().returns(throwError(() => new Error('network error')));
    const tagService = { ...defaultTagService(), bulkAddTag: bulkAddStub } as Partial<TagService>;
    const dialogRef = defaultDialogRef();

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    }).then(({ component }) => {
      cy.get('input[placeholder="Add tag…"]').type('vacation{enter}');
      cy.contains('Apply').click();
      cy.then(() => {
        expect(component.isSaving()).to.be.false;
      });
    });

    cy.get('@dialogClose').should('not.have.been.called');
  });

  it('should show a spinner and disable the buttons while saving', () => {
    const bulkAddStub = cy.stub().returns(new Subject<void>());
    const tagService = { ...defaultTagService(), bulkAddTag: bulkAddStub } as Partial<TagService>;

    cy.mount(BulkTagDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1] } },
      ],
    });

    cy.get('input[placeholder="Add tag…"]').type('vacation{enter}');
    cy.contains('Apply').click();
    cy.get('mat-spinner').should('exist');
    cy.contains('Cancel').should('be.disabled');
    cy.contains('Apply').should('be.disabled');
  });
});
