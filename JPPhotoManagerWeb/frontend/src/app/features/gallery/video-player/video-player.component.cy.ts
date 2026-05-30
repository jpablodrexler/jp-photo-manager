import { mount } from 'cypress/angular';
import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { VideoPlayerComponent } from './video-player.component';
import { MediaPlayerService } from '../../../core/services/media-player.service';

describe('VideoPlayerComponent', () => {
  function buildStub(): Partial<MediaPlayerService> {
    return {
      videoStreamUrl: signal(null),
      currentTrack: signal(null),
      currentTime: signal(0),
      duration: signal(0),
      isPlaying: signal(false),
      registerVideoElement: cy.stub(),
      seek: cy.stub(),
      prev: cy.stub(),
      stop: cy.stub(),
      togglePause: cy.stub(),
      next: cy.stub(),
    };
  }

  it('should render the video element', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    });

    cy.get('video').should('exist');
  });

  it('should call registerVideoElement on init', () => {
    const stub = buildStub();

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.wrap(stub.registerVideoElement).should('have.been.calledOnce');
  });

  it('should render all five control buttons', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    });

    cy.get('.video-controls button').should('have.length', 5);
  });

  it('should render the progress slider', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    });

    cy.get('input[type="range"]').should('exist');
  });
});
