import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FolderPickerDialogComponent } from './folder-picker-dialog.component';
import { FolderService } from '../../../core/services/folder.service';

const SOURCE_FOLDER = '/home/user/Pictures';
const OTHER_FOLDER = '/home/user/Downloads';

const defaultFolderService = (): Partial<FolderService> => ({
  getFolders: cy.stub().returns(of([
    { folderId: 1, path: SOURCE_FOLDER, name: 'Pictures', parentPath: null, children: [] },
    { folderId: 2, path: OTHER_FOLDER, name: 'Downloads', parentPath: null, children: [] },
  ])),
});

const defaultDialogRef = (): Partial<MatDialogRef<FolderPickerDialogComponent>> => ({
  close: cy.stub().as('dialogClose'),
});

const mountDialog = (mode: 'move' | 'copy', dialogRef = defaultDialogRef(), folderService = defaultFolderService()) => {
  return cy.mount(FolderPickerDialogComponent, {
    providers: [
      provideNoopAnimations(),
      { provide: FolderService, useValue: folderService },
      { provide: MatDialogRef, useValue: dialogRef },
      { provide: MAT_DIALOG_DATA, useValue: { mode, assetCount: 3, sourceFolder: SOURCE_FOLDER } },
    ],
  });
};

describe('FolderPickerDialogComponent', () => {
  it('should show the move title and button in move mode', () => {
    mountDialog('move');
    cy.contains('Move to Folder').should('be.visible');
    cy.contains('Move here').should('be.visible');
  });

  it('should show the copy title and button in copy mode', () => {
    mountDialog('copy');
    cy.contains('Copy to Folder').should('be.visible');
    cy.contains('Copy here').should('be.visible');
  });

  it('should disable the confirm button before a folder is selected', () => {
    mountDialog('move');
    cy.contains('Move here').should('be.disabled');
  });

  it('should enable the confirm button after a valid folder is selected', () => {
    mountDialog('move');
    cy.contains('Downloads').click();
    cy.contains('Move here').should('not.be.disabled');
  });

  it('should disable the confirm button when the destination equals the source', () => {
    mountDialog('move');
    cy.contains('Pictures').click();
    cy.contains('Move here').should('be.disabled');
  });

  it('should close the dialog with null when Cancel is clicked', () => {
    const dialogRef = defaultDialogRef();
    mountDialog('move', dialogRef);
    cy.contains('Cancel').click();
    cy.get('@dialogClose').should('have.been.calledWith', null);
  });
});
