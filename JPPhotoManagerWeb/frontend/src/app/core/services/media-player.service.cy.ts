import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MediaPlayerService } from './media-player.service';
import { Asset } from '../models/asset.model';

function makeAsset(fileName: string): Asset {
  return {
    assetId: 1,
    folderId: 1,
    folderPath: '/videos',
    fileName,
    fileSize: 1024,
    thumbnailCreationDateTime: '2024-01-01T00:00:00',
    hash: 'abc',
    thumbnailUrl: '/api/assets/1/thumbnail',
    imageUrl: '/api/assets/1/image',
    rating: 0,
    tags: [],
    fileType: 'VIDEO',
    isVideo: true,
  };
}

describe('MediaPlayerService', () => {
  let sut: MediaPlayerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MediaPlayerService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    sut = TestBed.inject(MediaPlayerService);
  });

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
