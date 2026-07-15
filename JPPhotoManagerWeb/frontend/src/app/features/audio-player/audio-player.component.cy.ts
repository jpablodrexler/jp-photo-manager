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

describe('AudioPlayerComponent', () => {
  it('renders_currentTrack_showsFileNameAndThumbnail', () => {
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: buildStub() }],
    });

    cy.contains('song.mp3').should('be.visible');
    cy.get('img.player-thumb').should('have.attr', 'src', '/api/assets/1/thumbnail');
  });

  it('renders_currentTimeAndDuration_formattedAsMinutesSeconds', () => {
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: buildStub() }],
    });

    cy.get('.player-time').first().should('contain', '1:05');
    cy.get('.player-time').last().should('contain', '3:20');
  });

  it('renders_isPlayingFalse_showsPlayIcon', () => {
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: buildStub({ isPlaying: signal(false) }) }],
    });

    cy.contains('mat-icon', 'play_arrow').should('be.visible');
  });

  it('renders_isPlayingTrue_showsPauseIcon', () => {
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: buildStub({ isPlaying: signal(true) }) }],
    });

    cy.contains('mat-icon', 'pause').should('be.visible');
  });

  it('clickPrevious_callsServicePrev', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('button[title="Previous"]').click();
    cy.wrap(stub.prev).should('have.been.calledOnce');
  });

  it('clickStop_callsServiceStop', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('button[title="Stop"]').click();
    cy.wrap(stub.stop).should('have.been.calledOnce');
  });

  it('clickPlayPause_callsServiceTogglePause', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('button[title="Play/Pause"]').click();
    cy.wrap(stub.togglePause).should('have.been.calledOnce');
  });

  it('clickNext_callsServiceNext', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('button[title="Next"]').click();
    cy.wrap(stub.next).should('have.been.calledOnce');
  });

  it('clickFullscreen_setsIsAudioFullscreenTrue', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('button[title="Fullscreen"]').click();
    cy.wrap(stub.isAudioFullscreen!).then(sig => {
      expect(sig()).to.equal(true);
    });
  });

  it('seekSlider_inputEvent_callsServiceSeek', () => {
    const stub = buildStub();
    cy.mount(AudioPlayerComponent, {
      providers: [provideNoopAnimations(), { provide: MediaPlayerService, useValue: stub }],
    });

    cy.get('input.player-slider').invoke('val', '90').trigger('input');
    cy.wrap(stub.seek).should('have.been.calledWith', 90);
  });
});
