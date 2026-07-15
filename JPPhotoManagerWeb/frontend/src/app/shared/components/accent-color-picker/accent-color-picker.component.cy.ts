import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AccentColorPickerComponent } from './accent-color-picker.component';

function mountPicker(componentProperties: Partial<AccentColorPickerComponent> = {}) {
  return cy.mount(AccentColorPickerComponent, {
    componentProperties,
    providers: [provideNoopAnimations()],
  });
}

describe('AccentColorPickerComponent', () => {
  it('should render eight swatches', () => {
    mountPicker();

    cy.get('.swatch').should('have.length', 8);
  });

  it('should mark the matching swatch active for the given accentColor', () => {
    mountPicker({ accentColor: '#2e7d32' });

    cy.get('.swatch').first().should('have.class', 'active');
    cy.get('.swatch').first().find('.check-icon').should('be.visible');
    cy.get('.swatch').not(':first').each($el => {
      cy.wrap($el).should('not.have.class', 'active');
    });
  });

  it('should emit accentColorSelected when a swatch is clicked', () => {
    mountPicker().then(({ fixture }) => {
      cy.spy(fixture.componentInstance.accentColorSelected, 'emit').as('selectedSpy');
      cy.get('.swatch').eq(1).click();
      cy.get('@selectedSpy').should('have.been.calledOnceWith', '#1565c0');
    });
  });
});
