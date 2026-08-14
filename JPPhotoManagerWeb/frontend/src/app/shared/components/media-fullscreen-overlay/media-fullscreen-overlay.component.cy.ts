import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MediaFullscreenOverlayComponent } from './media-fullscreen-overlay.component';
import { Asset } from '../../../core/models/asset.model';

const mockTrack: Partial<Asset> = { fileName: 'song.mp3', thumbnailUrl: '/api/assets/1/thumbnail' };

function mountOverlay(inputs: Partial<MediaFullscreenOverlayComponent> = {}) {
  return cy.mount(MediaFullscreenOverlayComponent, {
    componentProperties: {
      track: mockTrack as Asset,
      currentTime: 65,
      duration: 200,
      isPlaying: false,
      ...inputs,
    },
    providers: [provideNoopAnimations()],
  });
}

describe('MediaFullscreenOverlayComponent', () => {
  it('should show the file name and album art of the current track', () => {
    mountOverlay();

    cy.contains('.track-title', 'song.mp3').should('be.visible');
    cy.get('img.album-art').should('have.attr', 'src', '/api/assets/1/thumbnail');
    cy.get('img.album-art').should('have.attr', 'alt', 'song.mp3');
  });

  it('should format current time and duration as minutes:seconds', () => {
    mountOverlay();

    cy.get('.overlay-time').first().should('contain', '1:05');
    cy.get('.overlay-time').last().should('contain', '3:20');
  });

  it('should show 0:00 for a duration that is not finite', () => {
    mountOverlay({ duration: Infinity });

    cy.get('.overlay-time').last().should('contain', '0:00');
  });

  it('should show 0:00 for a duration that is NaN', () => {
    mountOverlay({ duration: NaN });

    cy.get('.overlay-time').last().should('contain', '0:00');
  });

  it('should show the play icon when not playing', () => {
    mountOverlay({ isPlaying: false });

    cy.get('.play-btn').contains('mat-icon', 'play_arrow').should('be.visible');
  });

  it('should show the pause icon when playing', () => {
    mountOverlay({ isPlaying: true });

    cy.get('.play-btn').contains('mat-icon', 'pause').should('be.visible');
  });

  it('should emit closed when the close button is clicked', () => {
    const onClosed = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.closed.subscribe(onClosed);
    });

    cy.get('.close-btn').click();
    cy.wrap(onClosed).should('have.been.calledOnce');
  });

  it('should emit previous when the previous button is clicked', () => {
    const onPrevious = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.previous.subscribe(onPrevious);
    });

    cy.get('button[title="Previous"]').click();
    cy.wrap(onPrevious).should('have.been.calledOnce');
  });

  it('should emit stopped when the stop button is clicked', () => {
    const onStopped = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.stopped.subscribe(onStopped);
    });

    cy.get('button[title="Stop"]').click();
    cy.wrap(onStopped).should('have.been.calledOnce');
  });

  it('should emit playPauseToggled when the play/pause button is clicked', () => {
    const onToggled = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.playPauseToggled.subscribe(onToggled);
    });

    cy.get('button[title="Play/Pause"]').click();
    cy.wrap(onToggled).should('have.been.calledOnce');
  });

  it('should emit nextTrack when the next button is clicked', () => {
    const onNext = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.nextTrack.subscribe(onNext);
    });

    cy.get('button[title="Next"]').click();
    cy.wrap(onNext).should('have.been.calledOnce');
  });

  it('should emit seekRequested with the slider value on a seek input event', () => {
    const onSeekRequested = cy.stub();
    mountOverlay().then(({ fixture }) => {
      fixture.componentInstance.seekRequested.subscribe(onSeekRequested);
    });

    cy.get('input.overlay-slider').invoke('val', '90').trigger('input');
    cy.wrap(onSeekRequested).should('have.been.calledWith', 90);
  });

  it('should render an empty track title when there is no track', () => {
    mountOverlay({ track: null });

    cy.get('.track-title').should('have.text', '');
    cy.get('img.album-art').should('exist');
  });
});
