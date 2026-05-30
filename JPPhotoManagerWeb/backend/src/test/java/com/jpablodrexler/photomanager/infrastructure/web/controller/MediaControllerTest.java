package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetPlaylistUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.StreamAssetUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock StreamAssetUseCase streamAssetUseCase;
    @Mock GetPlaylistUseCase getPlaylistUseCase;
    @Mock AssetWebMapper assetWebMapper;
    @InjectMocks MediaController sut;

    @TempDir
    Path tempDir;

    @Test
    void streamAsset_withRangeHeader_returns206PartialContent() throws Exception {
        Path audioFile = tempDir.resolve("track.mp3");
        Files.write(audioFile, new byte[10_000]);

        Folder folder = new Folder();
        folder.setPath(tempDir.toString());
        Asset asset = Asset.builder()
                .assetId(1L)
                .folder(folder)
                .fileName("track.mp3")
                .build();
        when(streamAssetUseCase.execute(1L)).thenReturn(asset);

        ResponseEntity<ResourceRegion> response = sut.streamAsset(1L, "bytes=0-4999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().get("Accept-Ranges")).containsExactly("bytes");
    }

    @Test
    void streamAsset_withoutRangeHeader_returns200Ok() throws Exception {
        Path audioFile = tempDir.resolve("track.mp3");
        Files.write(audioFile, new byte[1_000]);

        Folder folder = new Folder();
        folder.setPath(tempDir.toString());
        Asset asset = Asset.builder()
                .assetId(2L)
                .folder(folder)
                .fileName("track.mp3")
                .build();
        when(streamAssetUseCase.execute(2L)).thenReturn(asset);

        ResponseEntity<ResourceRegion> response = sut.streamAsset(2L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void streamAsset_mp4File_returnsVideoMp4ContentType() throws Exception {
        Path videoFile = tempDir.resolve("clip.mp4");
        Files.write(videoFile, new byte[1_000]);

        Folder folder = new Folder();
        folder.setPath(tempDir.toString());
        Asset asset = Asset.builder()
                .assetId(3L)
                .folder(folder)
                .fileName("clip.mp4")
                .build();
        when(streamAssetUseCase.execute(3L)).thenReturn(asset);

        ResponseEntity<ResourceRegion> response = sut.streamAsset(3L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("video/mp4");
    }

    @Test
    void streamAsset_webmFile_returnsVideoWebmContentType() throws Exception {
        Path videoFile = tempDir.resolve("clip.webm");
        Files.write(videoFile, new byte[1_000]);

        Folder folder = new Folder();
        folder.setPath(tempDir.toString());
        Asset asset = Asset.builder()
                .assetId(4L)
                .folder(folder)
                .fileName("clip.webm")
                .build();
        when(streamAssetUseCase.execute(4L)).thenReturn(asset);

        ResponseEntity<ResourceRegion> response = sut.streamAsset(4L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("video/webm");
    }
}
