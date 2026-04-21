import { mount } from 'cypress/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ThumbnailComponent } from './thumbnail.component';
import { Asset } from '../../../core/models/asset.model';

describe('ThumbnailComponent', () => {
  const mockAsset: Asset = {
    assetId: 1,
    folderId: 1,
    folderPath: '/photos',
    fileName: 'sunset.jpg',
    fileSize: 204800,
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
    hash: 'abc123',
    thumbnailUrl: '/api/assets/1/thumbnail',
    imageUrl: '/api/assets/1/image',
  };

  it('should render the asset file name', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.contains('.thumbnail-name', 'sunset.jpg');
  });

  it('should display the formatted file size', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.contains('.thumbnail-size', '200.0 KB');
  });

  it('should render the thumbnail image with correct src', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.get('img').should('have.attr', 'src', '/api/assets/1/thumbnail');
  });

  it('should apply selected class when selected input is true', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset, selected: true },
      providers: [provideNoopAnimations()],
    });

    cy.get('mat-card').should('have.class', 'selected');
  });

  it('should not apply selected class when selected input is false', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset, selected: false },
      providers: [provideNoopAnimations()],
    });

    cy.get('mat-card').should('not.have.class', 'selected');
  });

  it('should hide the image on load error', () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: { ...mockAsset, thumbnailUrl: '/bad-url' } },
      providers: [provideNoopAnimations()],
    });

    cy.get('img').then($img => {
      $img[0].dispatchEvent(new Event('error'));
    });
    cy.get('img').should('have.css', 'display', 'none');
  });
});
