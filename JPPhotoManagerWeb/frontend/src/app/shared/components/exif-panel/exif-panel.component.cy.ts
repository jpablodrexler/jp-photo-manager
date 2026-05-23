import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ExifPanelComponent } from './exif-panel.component';
import { AssetService } from '../../../core/services/asset.service';
import { TagService } from '../../../core/services/tag.service';
import { ExifMetadata } from '../../../core/models/exif-metadata.model';
import { Asset } from '../../../core/models/asset.model';

const makeAsset = (tags: string[] = []): Asset => ({
  assetId: 1,
  folderId: 10,
  folderPath: '/photos',
  fileName: 'test.jpg',
  fileSize: 1000,
  thumbnailCreationDateTime: '',
  hash: 'abc',
  thumbnailUrl: '',
  imageUrl: '',
  rating: 0,
  tags,
  fileType: 'IMAGE',
});

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

const defaultTagService = (): Partial<TagService> => ({
  addTag: cy.stub().returns(of(undefined)),
  removeTag: cy.stub().returns(of(undefined)),
  searchTags: cy.stub().returns(of([])),
});

describe('ExifPanelComponent', () => {
  it('panel_visibleFalse_panelContentHidden', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: false },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.get('.exif-panel').should('not.be.visible');
  });

  it('panel_visibleTrue_panelContentVisible', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.get('.exif-panel').should('be.visible');
  });

  it('panel_withExifData_displaysNonNullFields', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
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
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.contains('No EXIF data available').should('be.visible');
  });

  it('panel_closeButtonClick_emitsClosed', () => {
    const closedSpy = cy.spy().as('closedSpy');
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: {
        asset: makeAsset(),
        visible: true,
        closed: { emit: closedSpy } as any,
      },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.get('.exif-close').click();
    cy.get('@closedSpy').should('have.been.calledOnce');
  });

  it('panel_sameAssetOpenTwice_issuesOnlyOneHttpRequest', () => {
    const getExifStub = cy.stub().returns(of(exifWithData));
    const assetService = { getExifMetadata: getExifStub } as Partial<AssetService>;
    const asset = makeAsset();
    asset.assetId = 42;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    }).then(({ component }) => {
      component.visible = false;
      component.ngOnChanges({ visible: {} as any });
      component.visible = true;
      component.ngOnChanges({ visible: {} as any });
    });

    cy.wrap(getExifStub).should('have.been.calledOnce');
  });

  it('tags_displaysExistingTags', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(['vacation', 'family']), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.contains('vacation').should('be.visible');
    cy.contains('family').should('be.visible');
  });

  it('tags_addTagViaInput_callsServiceAndUpdatesChips', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const addTagStub = cy.stub().returns(of(undefined));
    const tagService = { ...defaultTagService(), addTag: addTagStub } as Partial<TagService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset([]), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: tagService },
      ],
    });

    cy.get('.exif-tag-input').type('beach{enter}');
    cy.wrap(addTagStub).should('have.been.calledWith', 1, 'beach');
    cy.contains('beach').should('be.visible');
  });

  it('tags_removeTag_callsServiceAndRemovesChip', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const removeTagStub = cy.stub().returns(of(undefined));
    const tagService = { ...defaultTagService(), removeTag: removeTagStub } as Partial<TagService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(['sunset']), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: tagService },
      ],
    });

    cy.contains('sunset').should('be.visible');
    cy.get('[matchipremove]').first().click();
    cy.wrap(removeTagStub).should('have.been.calledWith', 1, 'sunset');
    cy.contains('sunset').should('not.exist');
  });

  it('tags_autocomplete_showsSuggestionsOnInput', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const searchTagsStub = cy.stub().returns(of(['vacation', 'valley']));
    const tagService = { ...defaultTagService(), searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset([]), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: tagService },
      ],
    });

    cy.get('.exif-tag-input').type('va');
    cy.get('mat-option').should('have.length', 2);
    cy.get('mat-option').first().should('contain.text', 'vacation');
  });
});
