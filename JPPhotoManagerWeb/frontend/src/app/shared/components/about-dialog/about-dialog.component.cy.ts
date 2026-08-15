import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialogRef } from '@angular/material/dialog';
import { AboutDialogComponent } from './about-dialog.component';

describe('AboutDialogComponent', () => {
  it('should render the version and a link to the GitHub repo', () => {
    cy.mount(AboutDialogComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: { close: cy.stub() } },
      ],
    });

    cy.contains('1.0.0').should('exist');
    cy.get('a[href="https://github.com/jpablodrexler/jp-photo-manager"]').should('exist');
  });
});
