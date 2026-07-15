import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SocialMediaCropComponent } from './social-media-crop.component';
import { AssetService } from '../../../core/services/asset.service';
import { Asset } from '../../../core/models/asset.model';

// 1x1 transparent PNG, loaded inline so the component's `new Image()` load
// completes deterministically without a network request in headless CT.
const TINY_PNG_DATA_URL =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';

const mockAsset: Asset = {
  assetId: 42,
  folderId: 1,
  folderPath: '/photos',
  fileName: 'photo.jpg',
  fileSize: 1024,
  thumbnailCreationDateTime: '2024-01-01T00:00:00Z',
  hash: 'abc123',
  thumbnailUrl: '/api/assets/42/thumbnail',
  imageUrl: TINY_PNG_DATA_URL,
  rating: 0,
  tags: [],
  fileType: 'IMAGE',
  isVideo: false,
};

function buildAssetServiceStub(overrides: Partial<AssetService> = {}): Partial<AssetService> {
  return {
    cropAsset: cy.stub().returns(of({ ...mockAsset, assetId: 99 })),
    ...overrides,
  };
}

function mountCrop(assetServiceOverrides: Partial<AssetService> = {}) {
  const assetService = buildAssetServiceStub(assetServiceOverrides);
  return cy.mount(SocialMediaCropComponent, {
    componentProperties: { asset: mockAsset },
    providers: [
      provideNoopAnimations(),
      { provide: AssetService, useValue: assetService },
    ],
  }).then(({ component }) => ({ component, assetService }));
}

describe('SocialMediaCropComponent', () => {
  it('renders_withAsset_showsFormatSelectCanvasAndButtons', () => {
    mountCrop();
    cy.get('mat-select').should('exist');
    cy.get('canvas').should('exist');
    cy.contains('button', 'Save & Download').should('exist');
    cy.contains('button', 'Cancel').should('exist');
  });

  it('cancel_clickCancel_emitsCancelled', () => {
    mountCrop().then(({ component }) => {
      cy.spy(component.cancelled, 'emit').as('cancelledEmit');
      cy.contains('button', 'Cancel').click();
      cy.get('@cancelledEmit').should('have.been.calledOnce');
    });
  });

  it('formatSelect_choosingDifferentFormat_updatesSelectedFormat', () => {
    mountCrop().then(({ component }) => {
      cy.get('mat-select').click();
      cy.get('mat-option').contains('Facebook Post').click();
      cy.then(() => {
        expect(component.selectedFormat.key).to.equal('FACEBOOK_POST');
      });
    });
  });

  it('saveAndDownload_success_callsCropAssetWithSelectedFormatAndEmitsCancelled', () => {
    mountCrop().then(({ component, assetService }) => {
      cy.window().then(win => cy.stub(win, 'open'));
      cy.spy(component.cancelled, 'emit').as('cancelledEmit');
      cy.contains('button', 'Save & Download').click();
      cy.wrap(assetService.cropAsset).should('have.been.calledOnce');
      cy.wrap(assetService.cropAsset).then(stub => {
        const call = (stub as sinon.SinonStub).getCall(0);
        expect(call.args[0]).to.equal(42);
        expect(call.args[1].formatKey).to.equal('INSTAGRAM_POST');
      });
      cy.get('@cancelledEmit').should('have.been.calledOnce');
    });
  });

  it('saveAndDownload_error_showsFailureSnackbarMessage', () => {
    mountCrop({ cropAsset: cy.stub().returns(throwError(() => new Error('save failed'))) });
    cy.contains('button', 'Save & Download').click();
    cy.contains('Failed to save crop').should('be.visible');
  });
});
