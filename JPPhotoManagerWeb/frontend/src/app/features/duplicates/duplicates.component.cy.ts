import { mount } from 'cypress/angular';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DuplicatesComponent } from './duplicates.component';
import { AssetService } from '../../core/services/asset.service';
import { Asset } from '../../core/models/asset.model';
import { MockEventSource } from '../../../../cypress/support/mock-event-source';

describe('DuplicatesComponent', () => {
  const makeAsset = (id: number, fileName: string): Asset => ({
    assetId: id,
    folderId: 1,
    folderPath: '/photos',
    fileName,
    fileSize: 102400,
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
    hash: 'samehash',
    thumbnailUrl: `/api/assets/${id}/thumbnail`,
    imageUrl: `/api/assets/${id}/image`,
  });

  const mockDuplicateGroups: Asset[][] = [
    [makeAsset(1, 'photo_a.jpg'), makeAsset(2, 'photo_a_copy.jpg')],
    [makeAsset(3, 'sunset.jpg'), makeAsset(4, 'sunset_copy.jpg'), makeAsset(5, 'sunset_copy2.jpg')],
  ];

  function mountComponent(assetServiceOverrides: Partial<AssetService> = {}) {
    const assetServiceStub: Partial<AssetService> = {
      getDuplicatedAssets: cy.stub().returns(of(mockDuplicateGroups)),
      deleteAssets: cy.stub().returns(of(undefined)),
      catalogAssets: cy.stub().returns(new MockEventSource()),
      ...assetServiceOverrides,
    };

    return cy.mount(DuplicatesComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    }).then(result => ({ ...result, assetServiceStub }));
  }

  it('should create the component', () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it('should load duplicates on init', () => {
    const getDuplicatedAssets = cy.stub().returns(of(mockDuplicateGroups));
    mountComponent({ getDuplicatedAssets });
    cy.wrap(getDuplicatedAssets).should('have.been.calledOnce');
  });

  it('should display the correct number of duplicate groups', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      expect(fixture.componentInstance.groups).to.have.length(2);
    });
  });

  it('should calculate totalDuplicates correctly', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      expect(fixture.componentInstance.totalDuplicates).to.equal(3);
    });
  });

  it('should default keepIndex to 0 for each group', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      fixture.componentInstance.groups.forEach(group => {
        expect(group.keepIndex).to.equal(0);
      });
    });
  });

  it('should update keepIndex when markAsKeep is called', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      const component = fixture.componentInstance;
      const group = component.groups[0];
      component.markAsKeep(group, 1);
      expect(group.keepIndex).to.equal(1);
    });
  });

  it('should call deleteAssets with correct IDs when deleting a group', () => {
    const deleteAssets = cy.stub().returns(of(undefined));

    mountComponent({ deleteAssets }).then(({ fixture }) => {
      fixture.detectChanges();
      const component = fixture.componentInstance;
      const group = component.groups[0];
      component.deleteGroup(group);
      cy.wrap(deleteAssets).should('have.been.calledWith', [2], true);
    });
  });

  it('should remove the group from the list after successful deletion', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      const component = fixture.componentInstance;
      const initialLength = component.groups.length;
      component.deleteGroup(component.groups[0]);
      expect(component.groups).to.have.length(initialLength - 1);
    });
  });

  it('should delete the non-kept assets (not the kept one)', () => {
    const deleteAssets = cy.stub().returns(of(undefined));

    mountComponent({ deleteAssets }).then(({ fixture }) => {
      fixture.detectChanges();
      const component = fixture.componentInstance;
      const group = component.groups[1];
      component.markAsKeep(group, 1);
      component.deleteGroup(group);
      cy.wrap(deleteAssets).should('have.been.calledWith', [3, 5], true);
    });
  });

  it('assetItem_whenHovered_showsFullFilePath', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      cy.get('.asset-item').first().trigger('mouseenter', { bubbles: true });
      cy.get('.mat-mdc-tooltip').should('contain', '/photos/photo_a.jpg');
    });
  });

  it('assetItem_eachItem_hasTooltipWithFullPath', () => {
    mountComponent().then(({ fixture }) => {
      fixture.detectChanges();
      cy.get('.asset-item').each(($item, index) => {
        const allAssets = mockDuplicateGroups.flat();
        const asset = allAssets[index];
        cy.wrap($item)
          .should('have.attr', 'ng-reflect-message', `${asset.folderPath}/${asset.fileName}`);
      });
    });
  });

  it('should hide the loading spinner after duplicates are loaded', () => {
    mountComponent();
    cy.then(() => {
      cy.get('mat-progress-bar').should('not.exist');
    });
  });

  it('should show an error snackbar when loading duplicates fails', () => {
    mountComponent({ getDuplicatedAssets: cy.stub().returns(throwError(() => new Error('network'))) });
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Failed to load duplicates');
  });
});
