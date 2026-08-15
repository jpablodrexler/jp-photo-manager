import { of, throwError } from 'rxjs';
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
    return cy.mount(AlbumsComponent, {
      providers: [
        { provide: AlbumService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
  }

  it('should render a card for each album loaded on init', () => {
    mountAlbums();
    cy.get('mat-card').should('have.length', 2);
    cy.contains('Wedding').should('exist');
    cy.contains('Best of 2025').should('exist');
    cy.contains('42 photos').should('exist');
    cy.contains('7 photos').should('exist');
  });

  it('should call the delete service when the delete album button is clicked', () => {
    mountAlbums().then(({ serviceStub }) => {
      cy.get('button[title="Delete album"]').first().click();
      cy.wrap(serviceStub.deleteAlbum).should('have.been.calledWith', mockAlbums[0].albumId);
    });
  });

  it('should show the smart badge only on smart albums', () => {
    const albumsWithSmart: AlbumSummary[] = [
      { albumId: 1, name: 'Static Album', description: null, assetCount: 5, createdAt: '2024-01-01T00:00:00Z', filterJson: null },
      { albumId: 2, name: 'Smart Album', description: null, assetCount: 10, createdAt: '2024-01-01T00:00:00Z', filterJson: { minRating: 4 } }
    ];
    mountAlbums({
      getAlbums: cy.stub().returns(of(albumsWithSmart)),
      createAlbum: cy.stub().returns(of(albumsWithSmart[0])),
    });
    cy.get('mat-chip').should('have.length', 1).and('contain.text', 'Smart');
    cy.contains('Static Album').closest('mat-card').find('mat-chip').should('not.exist');
    cy.contains('Smart Album').closest('mat-card').find('mat-chip').should('contain.text', 'Smart');
  });

  it('should show filter fields when the smart toggle is enabled in the create album form', () => {
    const smartAlbum: AlbumSummary = { albumId: 3, name: 'Top Picks', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z', filterJson: { minRating: 4 } };
    const createStub = cy.stub().returns(of(smartAlbum));
    mountAlbums({
      getAlbums: cy.stub().returns(of([])),
      createAlbum: createStub,
    });
    cy.get('button[title="New album"]').click();
    cy.get('mat-slide-toggle button').click();
    cy.get('.smart-filter-fields').should('exist');
    cy.get('.create-form input').first().type('Top Picks');
    cy.get('button').contains('Create').click();
    cy.wrap(createStub).should('have.been.called');
  });

  it('should show the empty state when there are no albums', () => {
    mountAlbums({ getAlbums: cy.stub().returns(of([])) });
    cy.contains('.empty-state', 'No albums yet. Create one to get started.').should('exist');
  });

  it('should show a snackbar when loading albums fails', () => {
    mountAlbums({ getAlbums: cy.stub().returns(throwError(() => new Error('load failed'))) });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load albums');
  });

  it('should render the description when an album has one', () => {
    mountAlbums();
    cy.contains('Favourites').should('exist');
  });

  it('should pluralize the photo count for a single-asset album', () => {
    const oneAsset: AlbumSummary[] = [
      { albumId: 5, name: 'Solo', description: null, assetCount: 1, createdAt: '2024-01-01T00:00:00Z' },
    ];
    mountAlbums({ getAlbums: cy.stub().returns(of(oneAsset)) });
    cy.contains('1 photo').should('exist');
    cy.contains('1 photos').should('not.exist');
  });

  it('should not create an album when the name is blank', () => {
    mountAlbums({ getAlbums: cy.stub().returns(of([])) }).then(({ serviceStub }) => {
      cy.get('button[title="New album"]').click();
      cy.get('button').contains('Create').click();
      cy.wrap(serviceStub.createAlbum).should('not.have.been.called');
    });
  });

  it('should show a snackbar and keep the form open when creating an album fails', () => {
    mountAlbums({
      getAlbums: cy.stub().returns(of([])),
      createAlbum: cy.stub().returns(throwError(() => new Error('create failed'))),
    });
    cy.get('button[title="New album"]').click();
    cy.get('.create-form input').first().type('Broken Album');
    cy.get('button').contains('Create').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to create album');
    cy.get('.create-form').should('exist');
  });

  it('should show a success snackbar and reset the form when creating an album succeeds', () => {
    const created: AlbumSummary = { albumId: 9, name: 'Fresh', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z' };
    mountAlbums({
      getAlbums: cy.stub().returns(of([])),
      createAlbum: cy.stub().returns(of(created)),
    });
    cy.get('button[title="New album"]').click();
    cy.get('.create-form input').first().type('Fresh');
    cy.get('button').contains('Create').click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Album created');
    cy.get('.create-form').should('not.exist');
    cy.contains('Fresh').should('exist');
  });

  it('should include search, date range, and min rating in the smart album filter payload', () => {
    const smartAlbum: AlbumSummary = { albumId: 4, name: 'Rich Filter', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z' };
    const createStub = cy.stub().returns(of(smartAlbum));
    mountAlbums({ getAlbums: cy.stub().returns(of([])), createAlbum: createStub });

    cy.get('button[title="New album"]').click();
    cy.get('.create-form input').first().type('Rich Filter');
    cy.get('mat-slide-toggle button').click();
    // The outline mat-form-field's floating label overlaps this input
    // until focused, which Cypress's actionability check flags as
    // "covered" — force the type since the input is functionally reachable.
    cy.get('.smart-filter-fields input').first().type('sunset', { force: true });
    cy.get('.filter-star').eq(2).click();
    cy.get('button').contains('Create').click();

    cy.wrap(createStub).should('have.been.calledWith', Cypress.sinon.match({
      name: 'Rich Filter',
      filterJson: Cypress.sinon.match({ search: 'sunset', minRating: 3 }),
    }));
  });

  it('should format and include the date range in the smart album filter payload', () => {
    const smartAlbum: AlbumSummary = { albumId: 7, name: 'Date Ranged', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z' };
    const createStub = cy.stub().returns(of(smartAlbum));
    mountAlbums({ getAlbums: cy.stub().returns(of([])), createAlbum: createStub }).then(({ component }) => {
      component.makeSmartAlbum = true;
      component.smartDateFrom = new Date('2024-03-15T00:00:00Z');
      component.smartDateTo = new Date('2024-06-20T00:00:00Z');
      component.createAlbum('Date Ranged');

      cy.wrap(createStub).should('have.been.calledWith', Cypress.sinon.match({
        name: 'Date Ranged',
        filterJson: Cypress.sinon.match({ dateFrom: '2024-03-15', dateTo: '2024-06-20' }),
      }));
    });
  });

  it('should summarize every filter field and fall back to "No criteria" when unset', () => {
    mountAlbums().then(({ component }) => {
      expect(component.formatFilterSummary(null)).to.equal('No criteria');
      expect(component.formatFilterSummary({})).to.equal('No criteria');
      expect(component.formatFilterSummary({
        search: 'sunset', dateFrom: '2024-01-01', dateTo: '2024-12-31', minRating: 4
      })).to.equal('Search: sunset, From: 2024-01-01, To: 2024-12-31, Min rating: 4');
    });
  });

  it('should not attach a filterJson when the smart toggle is on but no criteria are set', () => {
    const smartAlbum: AlbumSummary = { albumId: 6, name: 'Empty Smart', description: null, assetCount: 0, createdAt: '2024-01-01T00:00:00Z' };
    const createStub = cy.stub().returns(of(smartAlbum));
    mountAlbums({ getAlbums: cy.stub().returns(of([])), createAlbum: createStub });

    cy.get('button[title="New album"]').click();
    cy.get('.create-form input').first().type('Empty Smart');
    cy.get('mat-slide-toggle button').click();
    cy.get('button').contains('Create').click();

    cy.wrap(createStub).should(stub => {
      const arg = stub.firstCall.args[0] as { name: string; filterJson?: unknown };
      expect(arg.name).to.equal('Empty Smart');
      expect(arg.filterJson).to.be.undefined;
    });
  });

  it('should show a snackbar and remove the album on successful delete', () => {
    mountAlbums();
    cy.get('button[title="Delete album"]').first().click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Album deleted');
    cy.contains('Wedding').should('not.exist');
  });

  it('should show a snackbar when deleting an album fails', () => {
    mountAlbums({ deleteAlbum: cy.stub().returns(throwError(() => new Error('delete failed'))) });
    cy.get('button[title="Delete album"]').first().click();
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to delete album');
    cy.contains('Wedding').should('exist');
  });
});
