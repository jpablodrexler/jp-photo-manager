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

  it('should format seconds as m:ss, zero-padding single-digit seconds', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      expect(component.formatTime(65)).to.equal('1:05');
      expect(component.formatTime(5)).to.equal('0:05');
      expect(component.formatTime(125)).to.equal('2:05');
      expect(component.formatTime(0)).to.equal('0:00');
    });
  });

  it('should format a non-finite duration as 0:00', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    }).then(({ fixture }) => {
      const component = fixture.componentInstance;
      expect(component.formatTime(NaN)).to.equal('0:00');
      expect(component.formatTime(Infinity)).to.equal('0:00');
    });
  });

  it('should call seek with the numeric slider value on input', () => {
    // duration drives the slider's [max] attribute, which the browser uses to
    // clamp any assigned value — needs to be > 42 or the range input clamps
    // the value back down to its max (0 from the default stub) before the
    // (input) handler ever reads it.
    const stub: Partial<MediaPlayerService> = { ...buildStub(), duration: signal(100) };

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.get('input[type="range"]').invoke('val', 42).trigger('input');
    cy.wrap(stub.seek).should('have.been.calledWith', 42);
  });

  it('should call mediaPlayer.prev when the previous button is clicked', () => {
    const stub = buildStub();

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.get('button[title="Previous"]').click();
    cy.wrap(stub.prev).should('have.been.calledOnce');
  });

  it('should call mediaPlayer.stop when the stop button is clicked', () => {
    const stub = buildStub();

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.get('button[title="Stop"]').click();
    cy.wrap(stub.stop).should('have.been.calledOnce');
  });

  it('should call mediaPlayer.togglePause when the play/pause button is clicked', () => {
    const stub = buildStub();

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.get('button[title="Play/Pause"]').click();
    cy.wrap(stub.togglePause).should('have.been.calledOnce');
  });

  it('should call mediaPlayer.next when the next button is clicked', () => {
    const stub = buildStub();

    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: stub },
      ],
    });

    cy.get('button[title="Next"]').click();
    cy.wrap(stub.next).should('have.been.calledOnce');
  });

  it('should call requestFullscreen on the video element when the fullscreen button is clicked', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    });

    // Stub on the real rendered DOM node (the same node the component's
    // `videoEl` ViewChild points to) rather than via `fixture.componentInstance`,
    // since Cypress guarantees this element already exists once queried.
    cy.get('video').then($video => {
      const stub = cy.stub($video[0], 'requestFullscreen').resolves(undefined);
      cy.wrap(stub).as('requestFullscreen');
    });

    cy.get('button[title="Fullscreen"]').click();
    cy.get('@requestFullscreen').should('have.been.calledOnce');
  });

  it('should not throw when requestFullscreen rejects', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    });

    cy.get('video').then($video => {
      cy.stub($video[0], 'requestFullscreen').rejects(new Error('not allowed'));
    });

    cy.get('button[title="Fullscreen"]').click();
    cy.get('button[title="Fullscreen"]').should('exist');
  });

  it('should register null with the media player on destroy', () => {
    cy.mount(VideoPlayerComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: MediaPlayerService, useValue: buildStub() },
      ],
    }).then(({ fixture }) => {
      const stub = fixture.componentInstance.mediaPlayer.registerVideoElement as unknown as sinon.SinonStub;
      fixture.componentInstance.ngOnDestroy();
      expect(stub).to.have.been.calledWith(null);
    });
  });
});
