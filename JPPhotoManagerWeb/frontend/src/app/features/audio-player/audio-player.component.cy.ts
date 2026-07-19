import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AudioPlayerComponent } from './audio-player.component';
import { MediaPlayerService } from '../../core/services/media-player.service';
import { Asset } from '../../core/models/asset.model';

const mockTrack: Partial<Asset> = { fileName: 'song.mp3', thumbnailUrl: '/api/assets/1/thumbnail' };

function buildStub(overrides: Partial<MediaPlayerService> = {}): Partial<MediaPlayerService> {
  return {
    currentTrack: signal(mockTrack as Asset),
    currentTime: signal(65),
    duration: signal(200),
    isPlaying: signal(false),
    isAudioFullscreen: signal(false),
    prev: cy.stub(),
    stop: cy.stub(),
    togglePause: cy.stub(),
    next: cy.stub(),
    seek: cy.stub(),
    ...overrides,
  };
}

function mountPlayer(overrides: Partial<MediaPlayerService> = {}): Partial<MediaPlayerService> {
  const stub = buildStub(overrides);
  cy.mount(AudioPlayerComponent, {
    providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
  });
  return stub;
}

describe('AudioPlayerComponent', () => {
  it('should show the file name and thumbnail of the current track', () => {
    mountPlayer();

    cy.contains('song.mp3').should('be.visible');
    cy.get('img.player-thumb').should('have.attr', 'src', '/api/assets/1/thumbnail');
  });

  it('should format current time and duration as minutes:seconds', () => {
    mountPlayer();

    cy.get('.player-time').first().should('contain', '1:05');
    cy.get('.player-time').last().should('contain', '3:20');
  });

  it('should show the play icon when not playing', () => {
    mountPlayer({ isPlaying: signal(false) });

    cy.contains('mat-icon', 'play_arrow').should('be.visible');
  });

  it('should show the pause icon when playing', () => {
    mountPlayer({ isPlaying: signal(true) });

    cy.contains('mat-icon', 'pause').should('be.visible');
  });

  it('should call the service prev method when the previous button is clicked', () => {
    const stub = mountPlayer();

    cy.get('button[title="Previous"]').click();
    cy.wrap(stub.prev).should('have.been.calledOnce');
  });

  it('should call the service stop method when the stop button is clicked', () => {
    const stub = mountPlayer();

    cy.get('button[title="Stop"]').click();
    cy.wrap(stub.stop).should('have.been.calledOnce');
  });

  it('should call the service togglePause method when the play/pause button is clicked', () => {
    const stub = mountPlayer();

    cy.get('button[title="Play/Pause"]').click();
    cy.wrap(stub.togglePause).should('have.been.calledOnce');
  });

  it('should call the service next method when the next button is clicked', () => {
    const stub = mountPlayer();

    cy.get('button[title="Next"]').click();
    cy.wrap(stub.next).should('have.been.calledOnce');
  });

  it('should set isAudioFullscreen to true when the fullscreen button is clicked', () => {
    const isAudioFullscreen = signal(false);
    mountPlayer({ isAudioFullscreen });

    cy.get('button[title="Fullscreen"]').click();
    cy.wrap(isAudioFullscreen).then(sig => {
      expect(sig()).to.equal(true);
    });
  });

  it('should call the service seek method on a seek slider input event', () => {
    const stub = mountPlayer();

    cy.get('input.player-slider').invoke('val', '90').trigger('input');
    cy.wrap(stub.seek).should('have.been.calledWith', 90);
  });
});
