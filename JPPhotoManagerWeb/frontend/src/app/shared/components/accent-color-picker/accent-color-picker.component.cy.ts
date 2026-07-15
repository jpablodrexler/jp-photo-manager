import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AccentColorPickerComponent } from './accent-color-picker.component';

describe('AccentColorPickerComponent', () => {
  it('mount_rendersEightSwatches', () => {
    cy.mount(AccentColorPickerComponent, {
      providers: [provideNoopAnimations()],
    });

    cy.get('.swatch').should('have.length', 8);
  });

  it('accentColorInput_marksMatchingSwatchActive', () => {
    cy.mount(AccentColorPickerComponent, {
      componentProperties: { accentColor: '#2e7d32' },
      providers: [provideNoopAnimations()],
    });

    cy.get('.swatch').first().should('have.class', 'active');
    cy.get('.swatch').first().find('.check-icon').should('be.visible');
    cy.get('.swatch').not(':first').each($el => {
      cy.wrap($el).should('not.have.class', 'active');
    });
  });

  it('swatchClick_emitsAccentColorSelected', () => {
    cy.mount(AccentColorPickerComponent, {
      providers: [provideNoopAnimations()],
    }).then(({ fixture }) => {
      cy.spy(fixture.componentInstance.accentColorSelected, 'emit').as('selectedSpy');
      cy.get('.swatch').eq(1).click();
      cy.get('@selectedSpy').should('have.been.calledOnceWith', '#1565c0');
    });
  });
});
