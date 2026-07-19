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

  it('should show the drop overlay on a dragover event', () => {
    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const mockEvent = { preventDefault: () => {} } as DragEvent;
      component.onDragOver(mockEvent);
      fixture.detectChanges();
    });
    cy.get('.drop-overlay').should('be.visible');
  });

  it('should hide the drop overlay on a dragleave event', () => {
    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const mockEvent = { preventDefault: () => {} } as DragEvent;
      component.onDragOver(mockEvent);
      fixture.detectChanges();
      component.onDragLeave();
      fixture.detectChanges();
    });
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
});
