import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MediaPlayerService } from './media-player.service';
import { Asset } from '../models/asset.model';

function makeAsset(fileName: string, assetId = 1): Asset {
  return {
    assetId,
    folderId: 1,
    folderPath: '/videos',
    fileName,
    fileSize: 1024,
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
    hash: 'abc',
    thumbnailUrl: `/api/assets/${assetId}/thumbnail`,
    imageUrl: `/api/assets/${assetId}/image`,
    rating: 0,
    tags: [],
    fileType: fileName.endsWith('.mp4') ? 'VIDEO' : 'AUDIO',
    isVideo: fileName.endsWith('.mp4'),
  };
}

function internals(sut: MediaPlayerService) {
  return sut as unknown as { audio: HTMLAudioElement; videoEl: HTMLVideoElement | null };
}

describe('MediaPlayerService', () => {
  let sut: MediaPlayerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MediaPlayerService,
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
      ],
    });
    sut = TestBed.inject(MediaPlayerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sut.stop();
  });

  describe('isVideoAsset', () => {
    it('should return true for .mp4', () => {
      expect(sut.isVideoAsset(makeAsset('clip.mp4'))).to.be.true;
    });

    it('should return true for .mov', () => {
      expect(sut.isVideoAsset(makeAsset('clip.mov'))).to.be.true;
    });

    it('should return true for .mkv', () => {
      expect(sut.isVideoAsset(makeAsset('clip.mkv'))).to.be.true;
    });

    it('should return true for .avi', () => {
      expect(sut.isVideoAsset(makeAsset('clip.avi'))).to.be.true;
    });

    it('should return true for .webm', () => {
      expect(sut.isVideoAsset(makeAsset('clip.webm'))).to.be.true;
    });

    it('should return true for uppercase extension', () => {
      expect(sut.isVideoAsset(makeAsset('clip.MP4'))).to.be.true;
    });

    it('should return false for .mp3', () => {
      expect(sut.isVideoAsset(makeAsset('track.mp3'))).to.be.false;
    });

    it('should return false for .jpg', () => {
      expect(sut.isVideoAsset(makeAsset('photo.jpg'))).to.be.false;
    });

    it('should return false for a file with no extension', () => {
      expect(sut.isVideoAsset(makeAsset('noextension'))).to.be.false;
    });
  });

  describe('play', () => {
    it('should set the queue, index and current track for an audio asset', () => {
      const track = makeAsset('track.mp3', 1);

      sut.play([track]);

      expect(sut.queue()).to.deep.equal([track]);
      expect(sut.currentIndex()).to.equal(0);
      expect(sut.currentTrack()).to.deep.equal(track);
      expect(sut.isVideoPlaying()).to.be.false;
      expect(sut.videoStreamUrl()).to.be.null;
      expect(internals(sut).audio.src).to.contain('/api/assets/1/stream');
    });

    it('should start at the given startIndex', () => {
      const first = makeAsset('one.mp3', 1);
      const second = makeAsset('two.mp3', 2);

      sut.play([first, second], 1);

      expect(sut.currentIndex()).to.equal(1);
      expect(sut.currentTrack()).to.deep.equal(second);
    });

    it('should set isVideoPlaying and the stream url for a video asset', () => {
      const clip = makeAsset('clip.mp4', 5);

      sut.play([clip]);

      expect(sut.isVideoPlaying()).to.be.true;
      expect(sut.videoStreamUrl()).to.contain('/api/assets/5/stream');
    });
  });

  describe('registerVideoElement', () => {
    it('should update isPlaying on the video element play/pause events', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);

      videoEl.dispatchEvent(new Event('play'));
      expect(sut.isPlaying()).to.be.true;

      videoEl.dispatchEvent(new Event('pause'));
      expect(sut.isPlaying()).to.be.false;
    });

    it('should update currentTime/duration on timeupdate only while playing video', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('clip.mp4', 1)]);
      expect(sut.isVideoPlaying()).to.be.true;

      videoEl.currentTime = 12;
      videoEl.dispatchEvent(new Event('timeupdate'));
      expect(sut.currentTime()).to.equal(12);

      sut.stop();
      videoEl.currentTime = 99;
      videoEl.dispatchEvent(new Event('timeupdate'));
      expect(sut.currentTime()).to.equal(0);
    });

    it('should call next() when the video element fires ended', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      const first = makeAsset('one.mp4', 1);
      const second = makeAsset('two.mp4', 2);
      sut.play([first, second], 0);

      videoEl.dispatchEvent(new Event('ended'));

      expect(sut.currentIndex()).to.equal(1);
      expect(sut.currentTrack()).to.deep.equal(second);
    });

    it('should clear the internal video element reference when passed null', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      expect(internals(sut).videoEl).to.equal(videoEl);

      sut.registerVideoElement(null);

      expect(internals(sut).videoEl).to.be.null;
    });
  });

  describe('audio element events', () => {
    it('should update isPlaying on play/pause', () => {
      const audio = internals(sut).audio;

      audio.dispatchEvent(new Event('play'));
      expect(sut.isPlaying()).to.be.true;

      audio.dispatchEvent(new Event('pause'));
      expect(sut.isPlaying()).to.be.false;
    });

    it('should update currentTime and duration on timeupdate', () => {
      const audio = internals(sut).audio;
      audio.currentTime = 42;

      audio.dispatchEvent(new Event('timeupdate'));

      expect(sut.currentTime()).to.equal(42);
      expect(sut.duration()).to.equal(0);
    });

    it('should call next() when the audio element fires ended', () => {
      const first = makeAsset('one.mp3', 1);
      const second = makeAsset('two.mp3', 2);
      sut.play([first, second], 0);

      internals(sut).audio.dispatchEvent(new Event('ended'));

      expect(sut.currentIndex()).to.equal(1);
      expect(sut.currentTrack()).to.deep.equal(second);
    });
  });

  describe('togglePause', () => {
    it('should call audio.play() when audio is paused and no video is playing', () => {
      const audio = internals(sut).audio;
      const play = cy.stub(audio, 'play').resolves();
      cy.stub(audio, 'paused').get(() => true);

      sut.togglePause();

      expect(play).to.have.been.calledOnce;
    });

    it('should call audio.pause() when audio is playing and no video is playing', () => {
      const audio = internals(sut).audio;
      const pause = cy.stub(audio, 'pause');
      cy.stub(audio, 'paused').get(() => false);

      sut.togglePause();

      expect(pause).to.have.been.calledOnce;
    });

    it('should call videoEl.play() when a video is playing and paused', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('clip.mp4', 1)]);
      const play = cy.stub(videoEl, 'play').resolves();
      cy.stub(videoEl, 'paused').get(() => true);

      sut.togglePause();

      expect(play).to.have.been.calledOnce;
    });

    it('should call videoEl.pause() when a video is playing and not paused', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('clip.mp4', 1)]);
      const pause = cy.stub(videoEl, 'pause');
      cy.stub(videoEl, 'paused').get(() => false);

      sut.togglePause();

      expect(pause).to.have.been.calledOnce;
    });

    it('should not throw when a video is playing but no video element is registered', () => {
      sut.play([makeAsset('clip.mp4', 1)]);

      expect(() => sut.togglePause()).to.not.throw();
    });
  });

  describe('stop', () => {
    it('should reset playback state and clear the audio source', () => {
      sut.play([makeAsset('track.mp3', 1)]);

      sut.stop();

      expect(sut.currentTrack()).to.be.null;
      expect(sut.queue()).to.deep.equal([]);
      expect(sut.isPlaying()).to.be.false;
      expect(sut.isVideoPlaying()).to.be.false;
      expect(sut.videoStreamUrl()).to.be.null;
      expect(sut.currentTime()).to.equal(0);
      expect(sut.duration()).to.equal(0);
      // Assigning `src = ''` on a real <audio> element resolves the read-back
      // value to the document's base URL rather than an empty string — assert
      // the previous stream path was cleared instead of an exact empty match.
      expect(internals(sut).audio.src).to.not.contain('/api/assets/1/stream');
    });

    it('should also clear a registered video element', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('clip.mp4', 1)]);

      sut.stop();

      expect(videoEl.src).to.not.contain('/api/assets/1/stream');
    });
  });

  describe('prev', () => {
    it('should restart the current track when more than 3 seconds have elapsed', () => {
      sut.play([makeAsset('one.mp3', 1), makeAsset('two.mp3', 2)], 1);
      internals(sut).audio.currentTime = 10;

      sut.prev();

      expect(internals(sut).audio.currentTime).to.equal(0);
      expect(sut.currentIndex()).to.equal(1);
    });

    it('should restart the current video when more than 3 seconds have elapsed', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('one.mp4', 1), makeAsset('two.mp4', 2)], 1);
      videoEl.currentTime = 10;

      sut.prev();

      expect(videoEl.currentTime).to.equal(0);
      expect(sut.currentIndex()).to.equal(1);
    });

    it('should move to the previous track when 3 or fewer seconds have elapsed', () => {
      const first = makeAsset('one.mp3', 1);
      const second = makeAsset('two.mp3', 2);
      sut.play([first, second], 1);
      internals(sut).audio.currentTime = 1;

      sut.prev();

      expect(sut.currentIndex()).to.equal(0);
      expect(sut.currentTrack()).to.deep.equal(first);
    });

    it('should do nothing when already at the first track', () => {
      const first = makeAsset('one.mp3', 1);
      sut.play([first], 0);
      internals(sut).audio.currentTime = 1;

      sut.prev();

      expect(sut.currentIndex()).to.equal(0);
      expect(sut.currentTrack()).to.deep.equal(first);
    });
  });

  describe('next', () => {
    it('should advance to the next track in the queue', () => {
      const first = makeAsset('one.mp3', 1);
      const second = makeAsset('two.mp3', 2);
      sut.play([first, second], 0);

      sut.next();

      expect(sut.currentIndex()).to.equal(1);
      expect(sut.currentTrack()).to.deep.equal(second);
    });

    it('should stop playback state when at the end of the queue', () => {
      sut.play([makeAsset('one.mp3', 1)], 0);

      sut.next();

      expect(sut.isPlaying()).to.be.false;
      expect(sut.isVideoPlaying()).to.be.false;
    });
  });

  describe('seek', () => {
    it('should set audio.currentTime when no video is playing', () => {
      sut.play([makeAsset('track.mp3', 1)]);

      sut.seek(33);

      expect(internals(sut).audio.currentTime).to.equal(33);
    });

    it('should set videoEl.currentTime when a video is playing', () => {
      const videoEl = document.createElement('video');
      sut.registerVideoElement(videoEl);
      sut.play([makeAsset('clip.mp4', 1)]);

      sut.seek(21);

      expect(videoEl.currentTime).to.equal(21);
    });
  });

  describe('loadFolder', () => {
    it('should GET /api/assets and return only AUDIO assets', () => {
      const audioAsset = makeAsset('track.mp3', 1);
      const videoAsset = makeAsset('clip.mp4', 2);

      sut.loadFolder('/music').subscribe(assets => {
        expect(assets).to.deep.equal([audioAsset]);
      });

      const req = httpMock.expectOne(r => r.url === '/api/assets');
      expect(req.request.params.get('folderPath')).to.equal('/music');
      expect(req.request.params.get('sort')).to.equal('FILE_NAME');
      expect(req.request.params.get('page')).to.equal('0');
      req.flush({ items: [audioAsset, videoAsset] });
    });
  });

  describe('loadPlaylist', () => {
    it('should GET /api/audio/playlist/:id', () => {
      const track = makeAsset('track.mp3', 1);

      sut.loadPlaylist(1).subscribe(assets => {
        expect(assets).to.deep.equal([track]);
      });

      const req = httpMock.expectOne('/api/audio/playlist/1');
      expect(req.request.method).to.equal('GET');
      req.flush([track]);
    });
  });
});
