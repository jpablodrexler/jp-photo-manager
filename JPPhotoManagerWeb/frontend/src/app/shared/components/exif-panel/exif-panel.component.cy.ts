import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ExifPanelComponent } from './exif-panel.component';
import { AssetService } from '../../../core/services/asset.service';
import { ExifMetadata } from '../../../core/models/exif-metadata.model';

const exifWithData: ExifMetadata = {
  cameraMake: 'TestCamera',
  cameraModel: 'ModelX',
  lensModel: null,
  exposureTime: '1/120',
  fNumber: 2.8,
  isoSpeed: 400,
  focalLength: 50,
  dateTaken: '2024-06-15T10:30:00',
  widthPixels: 4000,
  heightPixels: 3000,
  gpsLatitude: null,
  gpsLongitude: null,
};

const exifAllNull: ExifMetadata = {
  cameraMake: null,
  cameraModel: null,
  lensModel: null,
  exposureTime: null,
  fNumber: null,
  isoSpeed: null,
  focalLength: null,
  dateTaken: null,
  widthPixels: null,
  heightPixels: null,
  gpsLatitude: null,
  gpsLongitude: null,
};

describe('ExifPanelComponent', () => {
  it('panel_visibleFalse_panelContentHidden', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { assetId: 1, visible: false },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    });

    cy.get('.exif-panel').should('not.be.visible');
  });

  it('panel_visibleTrue_panelContentVisible', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { assetId: 1, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    });

    cy.get('.exif-panel').should('be.visible');
  });

  it('panel_withExifData_displaysNonNullFields', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { assetId: 1, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    });

    cy.contains('TestCamera').should('be.visible');
    cy.contains('ModelX').should('be.visible');
    cy.contains('400').should('be.visible');
    cy.contains('50mm').should('be.visible');
  });

  it('panel_allNullExif_showsNoDataMessage', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { assetId: 1, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    });

    cy.contains('No EXIF data available').should('be.visible');
  });

  it('panel_closeButtonClick_emitsClosed', () => {
    const closedSpy = cy.spy().as('closedSpy');
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: {
        assetId: 1,
        visible: true,
        closed: { emit: closedSpy } as any,
      },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    });

    cy.get('.exif-close').click();
    cy.get('@closedSpy').should('have.been.calledOnce');
  });

  it('panel_sameAssetOpenTwice_issuesOnlyOneHttpRequest', () => {
    const getExifStub = cy.stub().returns(of(exifWithData));
    const assetService = { getExifMetadata: getExifStub } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { assetId: 42, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
      ],
    }).then(({ component }) => {
      component.visible = false;
      component.ngOnChanges({ visible: {} as any });
      component.visible = true;
      component.ngOnChanges({ visible: {} as any });
    });

    cy.wrap(getExifStub).should('have.been.calledOnce');
  });
});
