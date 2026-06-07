import { mount } from 'cypress/angular';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { AlbumsComponent } from './albums.component';
import { AlbumService } from '../../core/services/album.service';
import { AlbumSummary } from '../../core/models/album.model';

describe('AlbumsComponent', () => {
  const mockAlbums: AlbumSummary[] = [
    { albumId: 1, name: 'Wedding', description: null, assetCount: 42, createdAt: '2024-01-01T00:00:00Z' },
    { albumId: 2, name: 'Best of 2025', description: 'Favourites', assetCount: 7, createdAt: '2025-01-01T00:00:00Z' }
  ];

  function mountAlbums(albumServiceOverrides: Partial<AlbumService> = {}) {
    const serviceStub: Partial<AlbumService> = {
      getAlbums: cy.stub().returns(of(mockAlbums)),
      createAlbum: cy.stub().returns(of(mockAlbums[0])),
      deleteAlbum: cy.stub().returns(of(undefined)),
      ...albumServiceOverrides
    };
    return mount(AlbumsComponent, {
      providers: [
        { provide: AlbumService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
  }

  it('ngOnInit_displaysAlbums_twoCardsRendered', () => {
    mountAlbums();
    cy.get('mat-card').should('have.length', 2);
    cy.contains('Wedding').should('exist');
    cy.contains('Best of 2025').should('exist');
    cy.contains('42 photos').should('exist');
    cy.contains('7 photos').should('exist');
  });

  it('deleteAlbum_callsServiceDelete_removesCard', () => {
    mountAlbums().then(({ serviceStub }) => {
      cy.get('button[title="Delete album"]').first().click();
      cy.wrap(serviceStub.deleteAlbum).should('have.been.calledWith', mockAlbums[0].albumId);
    });
  });

  it('smartAlbum_showsSmartBadge', () => {
    const albumsWithSmart: AlbumSummary[] = [
      { albumId: 1, name: 'Static Album', description: null, assetCount: 5, createdAt: '2024-01-01T00:00:00Z', filterJson: null },
      { albumId: 2, name: 'Smart Album', description: null, assetCount: 10, createdAt: '2024-01-01T00:00:00Z', filterJson: { minRating: 4 } }
    ];
    const serviceStub: Partial<AlbumService> = {
      getAlbums: cy.stub().returns(of(albumsWithSmart)),
      createAlbum: cy.stub().returns(of(albumsWithSmart[0])),
      deleteAlbum: cy.stub().returns(of(undefined))
    };
    mount(AlbumsComponent, {
      providers: [
        { provide: AlbumService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    });
    cy.get('mat-chip').should('have.length', 1).and('contain.text', 'Smart');
    cy.contains('Static Album').closest('mat-card').find('mat-chip').should('not.exist');
    cy.contains('Smart Album').closest('mat-card').find('mat-chip').should('contain.text', 'Smart');
  });

  it('createAlbumForm_smartToggle_showsFilterFields', () => {
    const smartAlbum: AlbumSummary = { albumId: 3, name: 'Top Picks', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z', filterJson: { minRating: 4 } };
    const createStub = cy.stub().returns(of(smartAlbum));
    const serviceStub: Partial<AlbumService> = {
      getAlbums: cy.stub().returns(of([])),
      createAlbum: createStub,
      deleteAlbum: cy.stub().returns(of(undefined))
    };
    mount(AlbumsComponent, {
      providers: [
        { provide: AlbumService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    });
    cy.get('button[title="New album"]').click();
    cy.get('mat-slide-toggle button').click();
    cy.get('.smart-filter-fields').should('exist');
    cy.get('.create-form input').first().type('Top Picks');
    cy.get('button').contains('Create').click();
    cy.wrap(createStub).should('have.been.called');
  });
});
