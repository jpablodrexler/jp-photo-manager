package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAssetItemProcessorTest {

    @Mock StoragePort storagePort;
    @Mock AudioMetadataService audioMetadataService;

    CatalogAssetItemProcessor sut;

    private static final ExifMetadata EMPTY_EXIF =
            new ExifMetadata(null, null, null, null, null, null, null, null, null, null, null, null, null);

    @Test
    void process_videoFile_tagsAsVideoWithFileTypeVideo() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/photos/clip.mp4");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("clip.mp4")).thenReturn(false);
        when(storagePort.isAudioFile("clip.mp4")).thenReturn(false);
        when(storagePort.isVideoFile("clip.mp4")).thenReturn(true);
        when(storagePort.getFileSize(filePathStr)).thenReturn(1024L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getImageRotation(filePathStr)).thenReturn(ImageRotation.ROTATE_0);
        when(storagePort.generateThumbnail(anyString(), anyInt(), anyInt())).thenReturn(new byte[]{1});
        when(storagePort.getExifMetadata(filePathStr)).thenReturn(EMPTY_EXIF);

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.asset().isVideo()).isTrue();
        assertThat(result.asset().getFileType()).isEqualTo(FileType.VIDEO);
    }

    @Test
    void process_imageFile_tagsAsImageWithFileTypeImage() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/photos/photo.jpg");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("photo.jpg")).thenReturn(false);
        when(storagePort.isAudioFile("photo.jpg")).thenReturn(false);
        when(storagePort.isVideoFile("photo.jpg")).thenReturn(false);
        when(storagePort.getFileSize(filePathStr)).thenReturn(1024L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getImageRotation(filePathStr)).thenReturn(ImageRotation.ROTATE_0);
        when(storagePort.generateThumbnail(anyString(), anyInt(), anyInt())).thenReturn(new byte[]{1});
        when(storagePort.getExifMetadata(filePathStr)).thenReturn(EMPTY_EXIF);

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.asset().isVideo()).isFalse();
        assertThat(result.asset().getFileType()).isEqualTo(FileType.IMAGE);
    }

    @Test
    void process_audioFile_tagsAsAudioAndSkipsVideoCheck() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/music/song.mp3");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("song.mp3")).thenReturn(false);
        when(storagePort.isAudioFile("song.mp3")).thenReturn(true);
        when(storagePort.getFileSize(filePathStr)).thenReturn(2048L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(audioMetadataService.extractAlbumArt(any())).thenReturn(Optional.empty());
        when(audioMetadataService.extract(any())).thenReturn(
                new AudioMetadata("Song", "Artist", "Album", 200, 320, 44100));

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.asset().getFileType()).isEqualTo(FileType.AUDIO);
        assertThat(result.asset().isVideo()).isFalse();
    }
}
