import { EventEmitter, SimpleChange } from '@angular/core';
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
  isVideo: false,
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
  rawExif: null,
};

const defaultTagService = (): Partial<TagService> => ({
  addTag: cy.stub().returns(of(undefined)),
  removeTag: cy.stub().returns(of(undefined)),
  searchTags: cy.stub().returns(of([])),
});

describe('ExifPanelComponent', () => {
  it('should hide the panel content when not visible', () => {
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

  it('should show the panel content when visible', () => {
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

  it('should display non-null EXIF fields', () => {
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

  it('should show a no-data message when all EXIF fields are null', () => {
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

  it('should emit closed when the close button is clicked', () => {
    const closedSpy = cy.spy().as('closedSpy');
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: {
        asset: makeAsset(),
        visible: true,
        closed: { emit: closedSpy } as unknown as EventEmitter<void>,
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

  it('should issue only one HTTP request when the same asset is opened twice', () => {
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
      component.ngOnChanges({ visible: new SimpleChange(true, false, false) });
      component.visible = true;
      component.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    });

    cy.wrap(getExifStub).should('have.been.calledOnce');
  });

  it('should display existing tags', () => {
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

  it('should call the service and update chips when adding a tag via input', () => {
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

  it('should call the service and remove the chip when removing a tag', () => {
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

  it('should show autocomplete suggestions on input', () => {
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

  it('should call the service and update chips when adding a tag via autocomplete selection', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const addTagStub = cy.stub().returns(of(undefined));
    const searchTagsStub = cy.stub().returns(of(['beach', 'boat']));
    const tagService = { ...defaultTagService(), addTag: addTagStub, searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset([]), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: tagService },
      ],
    });

    cy.get('.exif-tag-input').type('be');
    cy.get('mat-option').first().click();
    cy.wrap(addTagStub).should('have.been.calledWith', 1, 'beach');
    cy.contains('beach').should('be.visible');
  });

  it('should clear tag suggestions when the query is cleared back to empty', () => {
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
    cy.get('.exif-tag-input').clear();
    cy.get('mat-option').should('not.exist');
  });

  it('should revert the tag chip when adding a tag via autocomplete selection fails', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const addTagStub = cy.stub().returns(throwError(() => new Error('network error')));
    const searchTagsStub = cy.stub().returns(of(['beach']));
    const tagService = { ...defaultTagService(), addTag: addTagStub, searchTags: searchTagsStub } as Partial<TagService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset([]), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: tagService },
      ],
    });

    cy.get('.exif-tag-input').type('be');
    cy.get('mat-option').first().click();
    cy.contains('beach').should('not.exist');
  });

  it('should revert the tag chip when adding a tag fails', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const addTagStub = cy.stub().returns(throwError(() => new Error('network error')));
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
    cy.contains('beach').should('not.exist');
  });

  it('should restore the tag chip when removing a tag fails', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(null)) } as Partial<AssetService>;
    const removeTagStub = cy.stub().returns(throwError(() => new Error('network error')));
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
    cy.contains('sunset').should('be.visible');
  });

  it('should not fetch EXIF metadata when the panel starts hidden', () => {
    const getExifStub = cy.stub().returns(of(exifWithData));
    const assetService = { getExifMetadata: getExifStub } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: false },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.wrap(getExifStub).should('not.have.been.called');
  });

  it('should fetch EXIF metadata separately for two different assets', () => {
    const getExifStub = cy.stub().returns(of(exifWithData));
    const assetService = { getExifMetadata: getExifStub } as Partial<AssetService>;
    const assetOne = makeAsset();
    assetOne.assetId = 1;
    const assetTwo = makeAsset();
    assetTwo.assetId = 2;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: assetOne, visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    }).then(({ component }) => {
      component.asset = assetTwo;
      component.ngOnChanges({ asset: new SimpleChange(assetOne, assetTwo, false) });
    });

    cy.wrap(getExifStub).should('have.been.calledTwice');
    cy.wrap(getExifStub).should('have.been.calledWith', 1);
    cy.wrap(getExifStub).should('have.been.calledWith', 2);
  });

  it('should show a no-data message when fetching EXIF metadata fails', () => {
    const assetService = {
      getExifMetadata: cy.stub().returns(throwError(() => new Error('network error'))),
    } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.contains('No EXIF data available').should('be.visible');
    cy.get('mat-spinner').should('not.exist');
  });

  const exifWithRawData: ExifMetadata = {
    ...exifWithData,
    rawExif: { Make: 'TestCamera', Model: 'ModelX', ISOSpeedRatings: '400' },
  };

  it('should show all raw EXIF entries when the raw data panel is expanded', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithRawData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.contains('All EXIF data').click();
    cy.contains('.exif-raw-key', 'Make').should('be.visible');
    cy.contains('.exif-raw-key', 'Model').should('be.visible');
    cy.contains('.exif-raw-key', 'ISOSpeedRatings').should('be.visible');
  });

  it('should filter raw EXIF entries by the search field', () => {
    const assetService = { getExifMetadata: cy.stub().returns(of(exifWithRawData)) } as Partial<AssetService>;

    cy.mount(ExifPanelComponent, {
      componentProperties: { asset: makeAsset(), visible: true },
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetService },
        { provide: TagService, useValue: defaultTagService() },
      ],
    });

    cy.contains('All EXIF data').click();
    cy.get('.exif-filter-field input').type('iso');
    cy.contains('.exif-raw-key', 'ISOSpeedRatings').should('be.visible');
    cy.contains('.exif-raw-key', 'Make').should('not.exist');
  });
});
