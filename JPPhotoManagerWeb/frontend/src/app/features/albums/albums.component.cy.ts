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
});
