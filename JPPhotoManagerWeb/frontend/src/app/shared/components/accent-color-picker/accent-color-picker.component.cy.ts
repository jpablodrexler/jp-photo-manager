import { BehaviorSubject } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AccentColorPickerComponent } from './accent-color-picker.component';
import { ThemeService } from '../../../core/services/theme.service';

describe('AccentColorPickerComponent', () => {
  describe('with real ThemeService', () => {
    beforeEach(() => {
      cy.mount(AccentColorPickerComponent, {
        providers: [provideNoopAnimations()],
      });
    });

    it('mount_rendersEightSwatches', () => {
      cy.get('.swatch').should('have.length', 8);
    });

    it('swatchClick_updatesActiveClass', () => {
      cy.get('.swatch').eq(2).click();
      cy.get('.swatch').eq(2).should('have.class', 'active');
      cy.get('.swatch').not(':eq(2)').each($el => {
        cy.wrap($el).should('not.have.class', 'active');
      });
    });
  });

  it('mount_activeSwatchHasCheckIcon', () => {
    const accentSubject = new BehaviorSubject<string>('#2e7d32');
    const themeServiceStub: Partial<ThemeService> = {
      accentColor$: accentSubject.asObservable(),
      setAccentColor: cy.stub(),
    };

    cy.mount(AccentColorPickerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: ThemeService, useValue: themeServiceStub },
      ],
    });

    cy.get('.swatch').first().should('have.class', 'active');
    cy.get('.swatch').first().find('.check-icon').should('be.visible');
    cy.get('.swatch').not(':first').each($el => {
      cy.wrap($el).should('not.have.class', 'active');
    });
  });

  it('swatchClick_callsSetAccentColor', () => {
    const accentSubject = new BehaviorSubject<string>('#2e7d32');
    const setAccentColorStub = cy.stub();
    const themeServiceStub: Partial<ThemeService> = {
      accentColor$: accentSubject.asObservable(),
      setAccentColor: setAccentColorStub,
    };

    cy.mount(AccentColorPickerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: ThemeService, useValue: themeServiceStub },
      ],
    });

    cy.get('.swatch').eq(1).click();
    cy.wrap(setAccentColorStub).should('have.been.calledOnce');
    cy.wrap(setAccentColorStub).should('have.been.calledWith', '#1565c0');
  });
});
