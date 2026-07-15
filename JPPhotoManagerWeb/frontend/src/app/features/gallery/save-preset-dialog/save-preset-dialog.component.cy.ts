import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialogRef } from '@angular/material/dialog';
import { SavePresetDialogComponent } from './save-preset-dialog.component';

const defaultDialogRef = (): Partial<MatDialogRef<SavePresetDialogComponent, string>> => ({
  close: cy.stub().as('dialogClose'),
});

describe('SavePresetDialogComponent', () => {
  it('confirm_disabled_whenNameEmpty', () => {
    cy.mount(SavePresetDialogComponent, {
      providers: [provideNoopAnimations(), { provide: MatDialogRef, useValue: defaultDialogRef() }],
    });

    cy.contains('button', 'Save').should('be.disabled');
  });

  it('confirm_withName_closesDialogWithTrimmedName', () => {
    cy.mount(SavePresetDialogComponent, {
      providers: [provideNoopAnimations(), { provide: MatDialogRef, useValue: defaultDialogRef() }],
    });

    cy.get('input[matInput]').type('  My Preset  ');
    cy.contains('button', 'Save').should('not.be.disabled').click();
    cy.get('@dialogClose').should('have.been.calledWith', 'My Preset');
  });

  it('confirm_enterKeydown_closesDialog', () => {
    cy.mount(SavePresetDialogComponent, {
      providers: [provideNoopAnimations(), { provide: MatDialogRef, useValue: defaultDialogRef() }],
    });

    cy.get('input[matInput]').type('Quick Preset{enter}');
    cy.get('@dialogClose').should('have.been.calledWith', 'Quick Preset');
  });

  it('cancel_clickCancel_closesDialogWithNoArgs', () => {
    cy.mount(SavePresetDialogComponent, {
      providers: [provideNoopAnimations(), { provide: MatDialogRef, useValue: defaultDialogRef() }],
    });

    cy.contains('button', 'Cancel').click();
    cy.get('@dialogClose').should('have.been.called');
  });
});
