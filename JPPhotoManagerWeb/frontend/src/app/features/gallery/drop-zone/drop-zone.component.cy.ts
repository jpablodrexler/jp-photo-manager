import { of, Subject, throwError } from 'rxjs';
import { HttpEventType, HttpResponse, HttpUploadProgressEvent } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DropZoneComponent } from './drop-zone.component';
import { AssetService } from '../../../core/services/asset.service';
import { UploadAssetResponse } from '../../../core/models/asset.model';
import { MockEventSource } from '../../../../../cypress/support/mock-event-source';

function makeFile(name: string, type = 'image/jpeg'): File {
  return new File([new Uint8Array([0xff, 0xd8])], name, { type });
}

function makeFileList(files: File[]): FileList {
  const dt = new DataTransfer();
  files.forEach(f => dt.items.add(f));
  return dt.files;
}

describe('DropZoneComponent', () => {
  function mountDropZone(assetServiceOverrides: Partial<AssetService> = {}) {
    const uploadResponse: HttpResponse<UploadAssetResponse> = new HttpResponse({
      status: 202,
      body: { assetId: 42, status: 'PENDING' },
    });
    const assetServiceStub: Partial<AssetService> = {
      uploadAsset: cy.stub().returns(of(uploadResponse)),
      observeUpload: cy.stub().returns(new MockEventSource()),
      ...assetServiceOverrides,
    };

    return cy.mount(DropZoneComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetServiceStub },
      ],
      componentProperties: { folderPath: '/photos' },
    }).then(result => ({ ...result, assetServiceStub }));
  }

  it('should not show an upload queue when mounted with just a folder path', () => {
    mountDropZone();
    cy.get('.upload-queue').should('not.exist');
  });

  // Dispatched as real DOM events (not direct component.onDragOver()/onDragLeave() calls) so
  // they go through the @HostListener-wired Renderer2 dispatch the same way a real drag
  // interaction would, rather than mutating isDragging outside Angular's notified paths.
  it('should show the drop overlay on a dragover event', () => {
    mountDropZone();
    cy.get('[data-cy-root]').trigger('dragover');
    cy.get('.drop-overlay').should('be.visible');
  });

  it('should hide the drop overlay on a dragleave event', () => {
    mountDropZone();
    cy.get('[data-cy-root]').trigger('dragover');
    cy.get('.drop-overlay').should('be.visible');
    cy.get('[data-cy-root]').trigger('dragleave');
    cy.get('.drop-overlay').should('not.exist');
  });

  it('should not add a non-image file to the upload queue', () => {
    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('document.txt', 'text/plain')]));
      fixture.detectChanges();
    });
    cy.get('.upload-queue').should('not.exist');
  });

  it('should add a queue item and call uploadAsset for a valid JPEG file', () => {
    const uploadSubject = new Subject<unknown>();
    const uploadAsset = cy.stub().returns(uploadSubject.asObservable());

    mountDropZone({ uploadAsset } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
    });

    cy.wrap(uploadAsset).should('have.been.calledWith', '/photos', Cypress.sinon.match.instanceOf(File));
    cy.get('mat-progress-bar').should('exist');
  });

  it('should show a processing indicator and open the observe stream on a 202 Accepted response', () => {
    const observeUpload = cy.stub().returns(new MockEventSource());

    mountDropZone({ observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
    });

    cy.wrap(observeUpload).should('have.been.calledWith', 42);
    cy.get('.status-processing').should('contain.text', 'Processing');
  });

  it('should show a success icon and emit uploadComplete on an SSE done event', () => {
    const uploadComplete = cy.stub();
    const mockEventSource = new MockEventSource();
    const observeUpload = cy.stub().returns(mockEventSource);

    mountDropZone({ observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.uploadComplete.subscribe(uploadComplete);
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();

      mockEventSource.dispatchEvent(new MessageEvent('done', { data: '{}' }));
      fixture.detectChanges();
    });

    cy.wrap(uploadComplete).should('have.been.calledOnce');
    cy.get('.status-done').should('exist');
  });

  it('should show an error icon and emit uploadComplete on an SSE failed event', () => {
    const uploadComplete = cy.stub();
    const mockEventSource = new MockEventSource();
    const observeUpload = cy.stub().returns(mockEventSource);

    mountDropZone({ observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.uploadComplete.subscribe(uploadComplete);
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();

      mockEventSource.dispatchEvent(new MessageEvent('failed', { data: '{}' }));
      fixture.detectChanges();
    });

    cy.wrap(uploadComplete).should('have.been.calledOnce');
    cy.get('.status-error').should('exist');
  });

  it('should show an error icon and attempt no further upload on a 415 response', () => {
    const uploadAsset = cy.stub().returns(throwError(() => new Error('415 Unsupported Media Type')));

    mountDropZone({ uploadAsset } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
    });

    cy.wrap(uploadAsset).should('have.been.calledOnce');
    cy.get('.status-error').should('exist');
  });

  it('should upload a dropped file via a real drop DOM event', () => {
    const uploadAsset = cy.stub().returns(new Subject<unknown>().asObservable());

    mountDropZone({ uploadAsset } as Partial<AssetService>);

    cy.get('[data-cy-root]').then($el => {
      const dt = new DataTransfer();
      dt.items.add(makeFile('dropped.jpg'));
      const dropEvent = new DragEvent('drop', { bubbles: true, cancelable: true });
      Object.defineProperty(dropEvent, 'dataTransfer', { value: dt });
      $el[0].dispatchEvent(dropEvent);
    });

    cy.wrap(uploadAsset).should('have.been.calledOnce');
    cy.get('.drop-overlay').should('not.exist');
  });

  it('should open the file picker when the upload button is clicked', () => {
    mountDropZone();

    // Stub on the real rendered DOM node rather than via `fixture.componentInstance`'s
    // private `fileInput` ViewChild, since Cypress guarantees this element already
    // exists once queried.
    cy.get('input[type="file"]').then($input => {
      cy.stub($input[0], 'click').as('inputClick');
    });

    cy.get('button[title="Upload images"]').click();
    cy.get('@inputClick').should('have.been.calledOnce');
  });

  it('should upload a file selected via the hidden file input', () => {
    const uploadAsset = cy.stub().returns(new Subject<unknown>().asObservable());

    mountDropZone({ uploadAsset } as Partial<AssetService>);

    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from('fake-image-data'),
        fileName: 'selected.jpg',
        mimeType: 'image/jpeg',
      },
      { force: true },
    );

    cy.wrap(uploadAsset).should('have.been.calledOnce');
  });

  it('should update the progress bar value from upload progress events', () => {
    const uploadSubject = new Subject<HttpUploadProgressEvent>();
    const uploadAsset = cy.stub().returns(uploadSubject.asObservable());

    mountDropZone({ uploadAsset } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();

      uploadSubject.next({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 } as HttpUploadProgressEvent);
      fixture.detectChanges();
    });

    cy.get('mat-progress-bar').should('have.attr', 'aria-valuenow', '50');
  });

  it('should process files sequentially, uploading each queued file in turn', () => {
    const uploadAsset = cy
      .stub()
      .onFirstCall()
      .returns(of(new HttpResponse({ status: 202, body: { assetId: 1, status: 'PENDING' } })))
      .onSecondCall()
      .returns(of(new HttpResponse({ status: 202, body: { assetId: 2, status: 'PENDING' } })));
    const observeUpload = cy.stub().returns(new MockEventSource());

    mountDropZone({ uploadAsset, observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('a.jpg'), makeFile('b.jpg')]));
      fixture.detectChanges();
    });

    cy.wrap(uploadAsset).should('have.been.calledTwice');
    cy.get('.upload-item').should('have.length', 2);
  });

  it('should mark the item done directly when the upload response has no assetId', () => {
    const uploadResponse = new HttpResponse({
      status: 202,
      body: { status: 'PENDING' } as unknown as UploadAssetResponse,
    });
    const uploadAsset = cy.stub().returns(of(uploadResponse));
    const observeUpload = cy.stub();

    mountDropZone({ uploadAsset, observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
    });

    cy.get('.status-done').should('exist');
    cy.wrap(observeUpload).should('not.have.been.called');
  });

  it('should mark the item done when the observe stream connection drops', () => {
    const mockEventSource = new MockEventSource();
    const observeUpload = cy.stub().returns(mockEventSource);
    const uploadComplete = cy.stub();

    mountDropZone({ observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.uploadComplete.subscribe(uploadComplete);
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();

      // MockEventSource.dispatchEvent() only replays addEventListener-registered
      // listeners, not the onerror property the component assigns directly, so a
      // dropped SSE connection has to be simulated by invoking it directly. Cast
      // away the `this: EventSource` context requirement — this is a plain
      // callback invocation, not a real EventSource dispatch.
      (mockEventSource.onerror as ((event: Event) => void) | null)?.(new Event('error'));
      fixture.detectChanges();
    });

    cy.wrap(uploadComplete).should('have.been.calledOnce');
    cy.get('.status-done').should('exist');
  });

  it('should close any still-open observe streams on destroy', () => {
    const mockEventSource = new MockEventSource();
    const observeUpload = cy.stub().returns(mockEventSource);

    mountDropZone({ observeUpload } as Partial<AssetService>).then(({ fixture }) => {
      const closeSpy = cy.spy(mockEventSource, 'close').as('closeSpy');
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
      component.ngOnDestroy();
      cy.wrap(closeSpy).should('have.been.calledOnce');
    });
  });
});
