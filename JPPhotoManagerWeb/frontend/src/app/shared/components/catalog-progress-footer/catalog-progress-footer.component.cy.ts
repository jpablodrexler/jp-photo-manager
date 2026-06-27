import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CatalogProgressFooterComponent } from './catalog-progress-footer.component';
import { AssetService } from '../../../core/services/asset.service';
import { MockEventSource } from '../../../../../cypress/support/mock-event-source';

describe('CatalogProgressFooterComponent', () => {
  function mountComponent(assetServiceOverrides: Partial<AssetService> = {}) {
    const mockSource = new MockEventSource();
    const assetServiceStub: Partial<AssetService> = {
      observeCatalog: cy.stub().returns(mockSource),
      ...assetServiceOverrides,
    };

    return cy.mount(CatalogProgressFooterComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    }).then(result => {
      result.fixture.detectChanges();
      return { ...result, mockSource, assetServiceStub };
    });
  }

  it('should create the component', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('ngOnInit_called_connectsToObserveEndpoint', () => {
    mountComponent().then(({ assetServiceStub }) => {
      expect(assetServiceStub.observeCatalog).to.have.been.calledOnce;
    });
  });

  it('ngOnInit_called_startsInIdleState', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance.state).to.equal('idle');
    });
  });

  it('catalogEvent_received_switchesStateToRunning', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 10, folderPath: '/photos', reason: 'FOLDER_CREATED' });
      fixture.detectChanges();
      expect(fixture.componentInstance.state).to.equal('running');
    });
  });

  it('catalogEvent_withPercentCompleted_updatesPercentCompleted', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 75, folderPath: '/photos/vacation', reason: 'ASSET_CREATED' });
      fixture.detectChanges();
      expect(fixture.componentInstance.percentCompleted).to.equal(75);
    });
  });

  it('catalogEvent_withFolderPath_updatesStatusText', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 50, folderPath: '/photos/vacation', reason: 'FOLDER_CREATED' });
      fixture.detectChanges();
      expect(fixture.componentInstance.currentStatusText).to.equal('/photos/vacation');
    });
  });

  it('catalogEvent_withAssetFileName_updatesStatusTextFromFileName', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 30, asset: { fileName: 'photo.jpg' }, reason: 'ASSET_CREATED' });
      fixture.detectChanges();
      expect(fixture.componentInstance.currentStatusText).to.equal('photo.jpg');
    });
  });

  it('catalogDoneEvent_received_setsStateToIdle', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 100, folderPath: '/photos', reason: 'ASSET_CREATED' });
      fixture.detectChanges();
      mockSource.emitRaw('catalog-done', 'done');
      fixture.detectChanges();
      expect(fixture.componentInstance.state).to.equal('idle');
    });
  });

  it('catalogDoneEvent_received_recordsLastCompletedAt', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emitRaw('catalog-done', 'done');
      fixture.detectChanges();
      expect(fixture.componentInstance.lastCompletedAt).to.be.instanceOf(Date);
    });
  });

  it('onerror_whileRunning_setsStateToIdle', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 40, folderPath: '/photos', reason: 'ASSET_CREATED' });
      fixture.detectChanges();
      mockSource.onerror?.(new Event('error'));
      fixture.detectChanges();
      expect(fixture.componentInstance.state).to.equal('idle');
    });
  });

  it('onerror_whileIdle_keepsStateIdle', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.onerror?.(new Event('error'));
      fixture.detectChanges();
      expect(fixture.componentInstance.state).to.equal('idle');
    });
  });

  it('connect_whenCalled_opensNewObserveConnection', () => {
    mountComponent().then(({ fixture, assetServiceStub }) => {
      fixture.componentInstance.connect();
      expect(assetServiceStub.observeCatalog).to.have.been.calledTwice;
    });
  });

  it('runningState_showsProgressBarInTemplate', () => {
    mountComponent().then(({ fixture, mockSource }) => {
      mockSource.emit('catalog', { percentCompleted: 60, folderPath: '/photos', reason: 'ASSET_CREATED' });
      fixture.detectChanges();
      cy.get('mat-progress-bar').should('exist');
    });
  });

  it('idleState_doesNotShowProgressBar', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance.state).to.equal('idle');
      fixture.detectChanges();
      cy.get('mat-progress-bar').should('not.exist');
    });
  });

  it('idleState_displaysIdleText', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      cy.get('.catalog-status-text').should('contain.text', 'Idle');
    });
  });
});
