import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialogRef } from '@angular/material/dialog';
import { SavePresetDialogComponent } from './save-preset-dialog.component';

const defaultDialogRef = (): Partial<MatDialogRef<SavePresetDialogComponent, string>> => ({
  close: cy.stub().as('dialogClose'),
});

describe('SavePresetDialogComponent', () => {
  beforeEach(() => {
    cy.mount(SavePresetDialogComponent, {
      providers: [provideNoopAnimations(), { provide: MatDialogRef, useValue: defaultDialogRef() }],
    });
  });

  it('should disable Save when the name is empty', () => {
    cy.contains('button', 'Save').should('be.disabled');
  });

  it('should close the dialog with the trimmed name when Save is clicked', () => {
    cy.get('input[matInput]').type('  My Preset  ');
    cy.contains('button', 'Save').should('not.be.disabled').click();
    cy.get('@dialogClose').should('have.been.calledWith', 'My Preset');
  });

  it('should close the dialog on Enter keydown', () => {
    cy.get('input[matInput]').type('Quick Preset{enter}');
    cy.get('@dialogClose').should('have.been.calledWith', 'Quick Preset');
  });

  it('should close the dialog with no args when Cancel is clicked', () => {
    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.called');
  });
});
