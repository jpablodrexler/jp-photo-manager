import { mount } from 'cypress/angular';
import { of, Subject } from 'rxjs';
import { HttpEventType, HttpResponse, HttpUploadProgressEvent } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DropZoneComponent } from './drop-zone.component';
import { AssetService } from '../../../core/services/asset.service';

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
    const assetServiceStub: Partial<AssetService> = {
      uploadAsset: cy.stub().returns(of({ type: HttpEventType.Response, body: {} } as HttpResponse<unknown>)),
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

  it('mounts_withFolderPath_noUploadQueueVisible', () => {
    mountDropZone();
    cy.get('.upload-queue').should('not.exist');
  });

  it('dragover_event_setsDraggingAndShowsOverlay', () => {
    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const mockEvent = { preventDefault: () => {} } as DragEvent;
      component.onDragOver(mockEvent);
      fixture.detectChanges();
    });
    cy.get('.drop-overlay').should('be.visible');
  });

  it('dragleave_event_hidesDropOverlay', () => {
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

  it('onFilesSelected_nonImageFile_doesNotAddToQueue', () => {
    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.onFilesSelected(makeFileList([makeFile('document.txt', 'text/plain')]));
      fixture.detectChanges();
    });
    cy.get('.upload-queue').should('not.exist');
  });

  it('onFilesSelected_validJpeg_addsQueueItemAndCallsUploadAsset', () => {
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

  it('uploadComplete_onSuccess_emitsUploadComplete', () => {
    const uploadComplete = cy.stub();

    mountDropZone().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.uploadComplete.subscribe(uploadComplete);
      component.onFilesSelected(makeFileList([makeFile('photo.jpg')]));
      fixture.detectChanges();
    });

    cy.wrap(uploadComplete).should('have.been.called');
    cy.get('.status-done').should('exist');
  });
});
