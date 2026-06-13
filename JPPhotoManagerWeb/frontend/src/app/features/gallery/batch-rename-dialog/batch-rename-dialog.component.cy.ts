import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BatchRenameDialogComponent } from './batch-rename-dialog.component';
import { AssetService } from '../../../core/services/asset.service';
import { RenameAssetsResponse } from '../../../core/models/asset.model';

const previewResponse = (previews: { assetId: number; oldName: string; newName: string }[]): RenameAssetsResponse =>
  ({ previews, applied: false });

const defaultAssetService = (): Partial<AssetService> => ({
  renameAssets: cy.stub().returns(of(previewResponse([]))),
});

const defaultDialogRef = (): Partial<MatDialogRef<BatchRenameDialogComponent>> => ({
  close: cy.stub().as('dialogClose'),
});

const defaultData = { assetIds: [1, 2], assetCount: 2 };

describe('BatchRenameDialogComponent', () => {
  it('emptyPattern_applyButtonIsDisabled', () => {
    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: defaultAssetService() },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
      ],
    });

    cy.contains('button', 'Apply').should('be.disabled');
  });

  it('validPatternWithPreviews_applyButtonIsEnabled', () => {
    const renameStub = cy.stub().returns(of(previewResponse([
      { assetId: 1, oldName: 'a.jpg', newName: 'photo_001.jpg' },
      { assetId: 2, oldName: 'b.jpg', newName: 'photo_002.jpg' },
    ])));
    const assetService: Partial<AssetService> = { renameAssets: renameStub };

    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
      ],
    });

    cy.get('input[placeholder*="date"]').type('photo_{index:03d}.{ext}', { parseSpecialCharSequences: false });
    cy.wait(450);
    cy.contains('button', 'Apply').should('not.be.disabled');
  });

  it('collisionError_applyButtonDisabledAndErrorShown', () => {
    const renameStub = cy.stub().returns(throwError(() => ({ status: 400, error: { message: 'ASSET_NAME_COLLISION' } })));
    const assetService: Partial<AssetService> = { renameAssets: renameStub };

    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: MatDialogRef, useValue: defaultDialogRef() },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
      ],
    });

    cy.get('input[placeholder*="date"]').type('{date:yyyy-MM-dd}.{ext}', { parseSpecialCharSequences: false });
    cy.wait(450);
    cy.contains('Invalid pattern or name collision').should('be.visible');
    cy.contains('button', 'Apply').should('be.disabled');
  });

  it('cancel_closesDialogWithNull', () => {
    const dialogRef = defaultDialogRef();
    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: defaultAssetService() },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
      ],
    });

    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.calledWith', null);
  });

  it('applySuccess_closesDialogWithSuccessResult', () => {
    const previewStub = cy.stub();
    previewStub.withArgs([1, 2], 'new.{ext}', false).returns(of(previewResponse([
      { assetId: 1, oldName: 'a.jpg', newName: 'new.jpg' },
      { assetId: 2, oldName: 'b.jpg', newName: 'new.jpg' },
    ])));
    previewStub.withArgs([1, 2], 'new.{ext}', true).returns(of({ previews: [], applied: true }));

    const dialogRef = defaultDialogRef();
    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: { renameAssets: previewStub } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
      ],
    });

    cy.get('input[placeholder*="date"]').type('new.{ext}', { parseSpecialCharSequences: false });
    cy.wait(450);
    cy.contains('button', 'Apply').click();
    cy.get('@dialogClose').should('have.been.calledWith', { success: true, count: 2 });
  });

  it('applyError_closesDialogWithErrorResult', () => {
    const previewStub = cy.stub();
    previewStub.withArgs([1], 'test.{ext}', false).returns(of(previewResponse([
      { assetId: 1, oldName: 'a.jpg', newName: 'test.jpg' },
    ])));
    previewStub.withArgs([1], 'test.{ext}', true).returns(throwError(() => ({ error: { message: 'disk error' } })));

    const dialogRef = defaultDialogRef();
    cy.mount(BatchRenameDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: { renameAssets: previewStub } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { assetIds: [1], assetCount: 1 } },
      ],
    });

    cy.get('input[placeholder*="date"]').type('test.{ext}', { parseSpecialCharSequences: false });
    cy.wait(450);
    cy.contains('button', 'Apply').click();
    cy.get('@dialogClose').should('have.been.calledWith', { success: false, error: 'disk error' });
  });
});
