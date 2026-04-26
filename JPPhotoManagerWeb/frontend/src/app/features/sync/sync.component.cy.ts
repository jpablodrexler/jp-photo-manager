import { mount } from 'cypress/angular';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SyncComponent } from './sync.component';
import { SyncService } from '../../core/services/sync.service';
import { SyncAssetsDirectoriesDefinition, SyncAssetsResult } from '../../core/models/sync-config.model';
import { MockEventSource } from '../../../../cypress/support/mock-event-source';

describe('SyncComponent', () => {
  const mockDefinitions: SyncAssetsDirectoriesDefinition[] = [
    {
      id: 1,
      sourceDirectory: '/photos/source',
      destinationDirectory: '/photos/dest',
      includeSubFolders: true,
      deleteAssetsNotInSource: false,
      order: 0,
    },
  ];

  function mountComponent(syncServiceOverrides: Partial<SyncService> = {}) {
    const syncServiceStub: Partial<SyncService> = {
      getConfiguration: cy.stub().returns(of(mockDefinitions)),
      setConfiguration: cy.stub().returns(of(undefined)),
      run: cy.stub().returns(new MockEventSource()),
      ...syncServiceOverrides,
    };

    return cy.mount(SyncComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: SyncService, useValue: syncServiceStub },
      ],
    }).then(result => ({ ...result, syncServiceStub }));
  }

  it('should create the component', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should start on the configure step', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance.step).to.equal('configure');
    });
  });

  it('should load configuration on init', () => {
    const getConfiguration = cy.stub().returns(of(mockDefinitions));
    mountComponent({ getConfiguration });
    cy.wrap(getConfiguration).should('have.been.calledOnce');
  });

  it('should populate definitions from loaded configuration', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      expect(fixture.componentInstance.definitions).to.deep.equal(mockDefinitions);
    });
  });

  it('should add a new empty definition', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const initialLength = component.definitions.length;
      component.addDefinition();
      expect(component.definitions).to.have.length(initialLength + 1);
      expect(component.definitions[initialLength].sourceDirectory).to.equal('');
    });
  });

  it('should remove a definition by index', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.removeDefinition(0);
      expect(component.definitions).to.have.length(0);
    });
  });

  it('should move a definition up', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.definitions = [
        { ...mockDefinitions[0], order: 0 },
        { sourceDirectory: '/b', destinationDirectory: '/c', includeSubFolders: false, deleteAssetsNotInSource: false, order: 1 },
      ];
      component.moveUp(1);
      expect(component.definitions[0].sourceDirectory).to.equal('/b');
    });
  });

  it('should not move a definition up when already at the top', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const first = component.definitions[0];
      component.moveUp(0);
      expect(component.definitions[0]).to.equal(first);
    });
  });

  it('should move a definition down', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.definitions = [
        { ...mockDefinitions[0], order: 0 },
        { sourceDirectory: '/b', destinationDirectory: '/c', includeSubFolders: false, deleteAssetsNotInSource: false, order: 1 },
      ];
      component.moveDown(0);
      expect(component.definitions[0].sourceDirectory).to.equal('/b');
    });
  });

  it('should not move a definition down when already at the bottom', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      const last = component.definitions[component.definitions.length - 1];
      component.moveDown(component.definitions.length - 1);
      expect(component.definitions[component.definitions.length - 1]).to.equal(last);
    });
  });

  it('should transition to running step after saveAndRun', () => {
    const mockSource = new MockEventSource();
    const run = cy.stub().returns(mockSource);
    const setConfiguration = cy.stub().returns(of(undefined));

    mountComponent({ run, setConfiguration }).then(({ fixture }) => {
      fixture.componentInstance.saveAndRun();
      fixture.detectChanges();
      expect(fixture.componentInstance.step).to.equal('running');
    });
  });

  it('should transition to results step when SSE results event is received', () => {
    const mockSource = new MockEventSource();
    const mockResults: SyncAssetsResult[] = [
      { sourceDirectory: '/src', destinationDirectory: '/dst', syncedCount: 5, deletedCount: 0, success: true },
    ];

    mountComponent({ run: cy.stub().returns(mockSource), setConfiguration: cy.stub().returns(of(undefined)) })
      .then(({ fixture }) => {
        fixture.componentInstance.saveAndRun();
        fixture.detectChanges();

        mockSource.emitRaw('results', JSON.stringify(mockResults));
        fixture.detectChanges();

        expect(fixture.componentInstance.step).to.equal('results');
        expect(fixture.componentInstance.results).to.deep.equal(mockResults);
      });
  });

  it('should accumulate status messages from SSE', () => {
    const mockSource = new MockEventSource();

    mountComponent({ run: cy.stub().returns(mockSource), setConfiguration: cy.stub().returns(of(undefined)) })
      .then(({ fixture }) => {
        fixture.componentInstance.saveAndRun();
        fixture.detectChanges();

        mockSource.emitRaw('status', 'Processing file 1');
        mockSource.emitRaw('status', 'Processing file 2');
        fixture.detectChanges();

        expect(fixture.componentInstance.statusMessages).to.deep.equal(['Processing file 1', 'Processing file 2']);
      });
  });

  it('should return to configure step when backToConfigure is called', () => {
    mountComponent().then(({ fixture }) => {
      const component = fixture.componentInstance;
      component.step = 'results';
      component.backToConfigure();
      expect(component.step).to.equal('configure');
    });
  });

  it('should show an error snackbar when configuration load fails', () => {
    mountComponent({ getConfiguration: cy.stub().returns(throwError(() => new Error('network'))) });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load configuration');
  });
});
