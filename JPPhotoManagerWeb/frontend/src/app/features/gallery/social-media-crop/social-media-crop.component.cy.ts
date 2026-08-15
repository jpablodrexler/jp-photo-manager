import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SocialMediaCropComponent } from './social-media-crop.component';
import { AssetService } from '../../../core/services/asset.service';
import { Asset } from '../../../core/models/asset.model';
import { SOCIAL_MEDIA_FORMATS } from '../../../core/models/social-media-format.model';

// Overrides the canvas's intrinsic pixel size and stubs its bounding rect to a
// known, zero-offset box so `getCanvasCoords()` maps clientX/Y 1:1 to canvas
// pixel coordinates — makes crop-box hit-testing/drag math deterministic
// regardless of the real browser viewport or the CSS `max-width`/`max-height`
// on the canvas (social-media-crop.component.scss).
function primeCanvas(canvas: HTMLCanvasElement, size = 400): HTMLCanvasElement {
  canvas.width = size;
  canvas.height = size;
  Cypress.sinon.stub(canvas, 'getBoundingClientRect').returns({
    left: 0,
    top: 0,
    right: size,
    bottom: size,
    width: size,
    height: size,
    x: 0,
    y: 0,
    toJSON: () => undefined,
  } as DOMRect);
  return canvas;
}

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
  }).then(({ component, fixture }) => {
    // This app is zoneless, and cy.mount() here comes from the plain (not
    // -zoneless) `cypress/angular` mount helper (see the
    // cypress-unit-test-developer skill §1.2) — its single implicit
    // `detectChanges()` isn't always enough to resolve a non-static
    // `@ViewChild` (like `canvasRef` below) before test code's very next
    // synchronous line runs. An explicit extra `detectChanges()` here forces
    // that resolution deterministically instead of relying on timing.
    fixture.detectChanges();
    return { component, assetService, fixture };
  });
}

// Grabs the canvas via `cy.get('canvas')` (a real, always-rendered DOM node)
// rather than `component.canvasRef.nativeElement` directly, since Cypress
// guarantees it via its own retry/query mechanism.
function mountCropWithPrimedCanvas(size = 400) {
  return mountCrop().then(({ component, assetService }) =>
    cy.get('canvas').then($canvas => {
      const canvas = primeCanvas($canvas[0] as HTMLCanvasElement, size);
      // The real (pre-override) initCanvas() already computed a crop box
      // sized against the canvas's original, environment-dependent
      // dimensions — force a recompute against the freshly-overridden size
      // so it actually reflects the primed canvas, not a stale one.
      component.onFormatChange();
      return { component, assetService, canvas };
    }),
  );
}

describe('SocialMediaCropComponent', () => {
  it('should show the format select, canvas, and action buttons for an asset', () => {
    mountCrop();
    cy.get('mat-select').should('exist');
    cy.get('canvas').should('exist');
    cy.contains('button', 'Save & Download').should('exist');
    cy.contains('button', 'Cancel').should('exist');
  });

  it('should emit cancelled when the Cancel button is clicked', () => {
    mountCrop().then(({ component }) => {
      cy.spy(component.cancelled, 'emit').as('cancelledEmit');
      cy.contains('button', 'Cancel').click();
      cy.get('@cancelledEmit').should('have.been.calledOnce');
    });
  });

  it('should update the selected format when a different format is chosen', () => {
    mountCrop().then(({ component }) => {
      cy.get('mat-select').click();
      cy.get('mat-option').contains('Facebook Post').click();
      cy.then(() => {
        expect(component.selectedFormat.key).to.equal('FACEBOOK_POST');
      });
    });
  });

  it('should call cropAsset with the selected format and emit cancelled on successful save', () => {
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

  it('should show a failure snackbar message when saving the crop fails', () => {
    mountCrop({ cropAsset: cy.stub().returns(throwError(() => new Error('save failed'))) });
    cy.contains('button', 'Save & Download').click();
    cy.contains('Failed to save crop').should('be.visible');
  });

  // --- Crop-box hit-testing (cursor) tests ---
  // The default format (INSTAGRAM_POST, 1:1) fills the whole primed 400x400
  // canvas, so its crop box spans (0,0)-(400,400).

  it('should set the cursor to move when hovering inside the crop box', () => {
    mountCropWithPrimedCanvas().then(({ component }) => {
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 200, clientY: 200 }));
    });
    cy.get('canvas').should('have.css', 'cursor', 'move');
  });

  it('should set the cursor to nw-resize over the top-left corner handle', () => {
    mountCropWithPrimedCanvas().then(({ component }) => {
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 5, clientY: 5 }));
    });
    cy.get('canvas').should('have.css', 'cursor', 'nw-resize');
  });

  it('should set the cursor to nw-resize over the bottom-right corner handle', () => {
    mountCropWithPrimedCanvas().then(({ component }) => {
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 395, clientY: 395 }));
    });
    cy.get('canvas').should('have.css', 'cursor', 'nw-resize');
  });

  it('should set the cursor to ne-resize over the top-right corner handle', () => {
    mountCropWithPrimedCanvas().then(({ component }) => {
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 395, clientY: 5 }));
    });
    cy.get('canvas').should('have.css', 'cursor', 'ne-resize');
  });

  it('should set the cursor to crosshair when hovering outside the crop box', () => {
    mountCropWithPrimedCanvas().then(({ component }) => {
      // Switch to a non-1:1 format so the crop box no longer fills the whole
      // square canvas, leaving a margin outside it to hover over.
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'INSTAGRAM_PORTRAIT')!;
      component.onFormatChange();
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 10, clientY: 200 }));
    });
    cy.get('canvas').should('have.css', 'cursor', 'crosshair');
  });

  // --- Crop-box drag/resize tests ---
  // Verified via the border stroked by redraw() (ctx.strokeRect(x+1, y+1,
  // w-2, h-2)) rather than the eventual crop request, since the mock asset's
  // 1x1 test image makes saveAndDownload()'s natural-size scaling round
  // everything down to ~0px and so can't distinguish drag outcomes.

  it('should move the crop box within canvas bounds when dragging from inside it', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'INSTAGRAM_PORTRAIT')!;
      component.onFormatChange(); // crop box becomes {x:40, y:0, w:320, h:400}
      const ctx = canvas.getContext('2d')!;
      const strokeRectSpy = Cypress.sinon.spy(ctx, 'strokeRect');

      component.onMouseDown(new MouseEvent('mousedown', { clientX: 200, clientY: 200 }));
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 170, clientY: 200 })); // 30px left
      component.onMouseUp();

      cy.wrap(strokeRectSpy).then(spy => {
        const lastCall = (spy as unknown as sinon.SinonSpy).lastCall;
        expect(lastCall.args).to.deep.equal([11, 1, 318, 398]);
      });
    });
  });

  it('should clamp the crop box to the canvas edge when dragging past its bounds', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'INSTAGRAM_PORTRAIT')!;
      component.onFormatChange(); // crop box becomes {x:40, y:0, w:320, h:400}
      const ctx = canvas.getContext('2d')!;
      const strokeRectSpy = Cypress.sinon.spy(ctx, 'strokeRect');

      component.onMouseDown(new MouseEvent('mousedown', { clientX: 200, clientY: 200 }));
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 600, clientY: 200 })); // far past the right edge
      component.onMouseUp();

      cy.wrap(strokeRectSpy).then(spy => {
        const lastCall = (spy as unknown as sinon.SinonSpy).lastCall;
        // x clamps at canvas.width - w = 400 - 320 = 80
        expect(lastCall.args).to.deep.equal([81, 1, 318, 398]);
      });
    });
  });

  it('should resize the crop box from a corner handle while preserving the format aspect ratio', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      const ctx = canvas.getContext('2d')!;
      const strokeRectSpy = Cypress.sinon.spy(ctx, 'strokeRect');

      component.onMouseDown(new MouseEvent('mousedown', { clientX: 395, clientY: 395 })); // grab BR handle
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 300, clientY: 300 }));
      component.onMouseUp();

      cy.wrap(strokeRectSpy).then(spy => {
        const lastCall = (spy as unknown as sinon.SinonSpy).lastCall;
        expect(lastCall.args).to.deep.equal([1, 1, 298, 298]);
      });
    });
  });

  it('should not start a drag when mousedown occurs outside any hit area', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'INSTAGRAM_PORTRAIT')!;
      component.onFormatChange();
      const ctx = canvas.getContext('2d')!;
      const strokeRectSpy = Cypress.sinon.spy(ctx, 'strokeRect');

      component.onMouseDown(new MouseEvent('mousedown', { clientX: 10, clientY: 200 })); // outside the box
      component.onMouseMove(new MouseEvent('mousemove', { clientX: 300, clientY: 200 }));

      cy.wrap(strokeRectSpy).should('not.have.been.called');
    });
  });

  it('should stop dragging on mouse up so a subsequent mouse move no longer resizes the crop box', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      component.onMouseDown(new MouseEvent('mousedown', { clientX: 395, clientY: 395 }));
      component.onMouseUp();
      const ctx = canvas.getContext('2d')!;
      const strokeRectSpy = Cypress.sinon.spy(ctx, 'strokeRect');

      component.onMouseMove(new MouseEvent('mousemove', { clientX: 300, clientY: 300 }));

      cy.wrap(strokeRectSpy).should('not.have.been.called');
    });
  });

  // --- Circular-format guide overlay tests ---

  it('should draw a circular guide overlay when the selected format is circular', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      const ctx = canvas.getContext('2d')!;
      const arcSpy = Cypress.sinon.spy(ctx, 'arc');
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'INSTAGRAM_PROFILE')!;
      component.onFormatChange();

      cy.wrap(arcSpy).should('have.been.called');
    });
  });

  it('should not draw a circular guide overlay when the selected format is not circular', () => {
    mountCropWithPrimedCanvas().then(({ component, canvas }) => {
      const ctx = canvas.getContext('2d')!;
      const arcSpy = Cypress.sinon.spy(ctx, 'arc');
      component.selectedFormat = SOCIAL_MEDIA_FORMATS.find(f => f.key === 'FACEBOOK_POST')!;
      component.onFormatChange();

      cy.wrap(arcSpy).should('not.have.been.called');
    });
  });
});
