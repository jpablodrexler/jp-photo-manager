import { EventEmitter } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TimelineViewComponent } from './timeline-view.component';
import { TimelineGroup } from '../../../core/models/timeline-group.model';
import { Asset } from '../../../core/models/asset.model';

describe('TimelineViewComponent', () => {
  const mockAsset: Asset = {
    assetId: 1,
    folderId: 1,
    folderPath: '/photos',
    fileName: 'sunset.jpg',
    fileSize: 1024000,
    thumbnailCreationDateTime: '2024-05-10T10:00:00',
    hash: 'abc123',
    thumbnailUrl: '/api/assets/1/thumbnail',
    imageUrl: '/api/assets/1/image',
    rating: 3,
    tags: [],
    fileType: 'IMAGE',
    isVideo: false,
  };

  const mockGroups: TimelineGroup[] = [
    {
      localDate: '2024-05-10',
      label: 'May 10, 2024',
      assets: [mockAsset],
    },
    {
      localDate: '2024-04-20',
      label: 'April 20, 2024',
      assets: [{ ...mockAsset, assetId: 2, fileName: 'beach.jpg' }],
    },
  ];

  it('should render groups with the correct month and day headers', () => {
    cy.mount(TimelineViewComponent, {
      componentProperties: { groups: mockGroups },
      providers: [provideNoopAnimations()],
    });

    cy.get('.timeline-month-header').should('have.length', 2);
    cy.get('.timeline-month-header').first().should('contain.text', 'May 2024');
    cy.get('.timeline-month-header').last().should('contain.text', 'April 2024');
    cy.get('.timeline-day-header').should('have.length', 2);
    cy.get('.timeline-thumbnail-cell').should('have.length', 2);
  });

  it('should show the empty state when there are no groups', () => {
    cy.mount(TimelineViewComponent, {
      componentProperties: { groups: [] },
      providers: [provideNoopAnimations()],
    });

    cy.get('.timeline-empty').should('be.visible');
    cy.get('.timeline-container').should('not.exist');
  });

  it('should emit thumbnailClick when a thumbnail is clicked', () => {
    const clickSpy = cy.spy().as('clickSpy');

    cy.mount(TimelineViewComponent, {
      componentProperties: {
        groups: [mockGroups[0]],
        thumbnailClick: { emit: clickSpy } as unknown as EventEmitter<Asset>,
      },
      providers: [provideNoopAnimations()],
    });

    cy.get('.timeline-thumbnail-cell').first().click();
    cy.get('@clickSpy').should('have.been.calledOnce');
  });

  it('should share one month header across groups in the same month', () => {
    const sameMonthGroups: TimelineGroup[] = [
      { localDate: '2024-05-10', label: 'May 10, 2024', assets: [mockAsset] },
      { localDate: '2024-05-09', label: 'May 9, 2024', assets: [{ ...mockAsset, assetId: 2 }] },
    ];

    cy.mount(TimelineViewComponent, {
      componentProperties: { groups: sameMonthGroups },
      providers: [provideNoopAnimations()],
    });

    cy.get('.timeline-month-header').should('have.length', 1);
    cy.get('.timeline-day-header').should('have.length', 2);
  });
});
