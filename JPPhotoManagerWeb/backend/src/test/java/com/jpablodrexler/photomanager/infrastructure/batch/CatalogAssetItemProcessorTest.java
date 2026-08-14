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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    @Test
    void process_playlistFile_tagsAsPlaylistAndSkipsExifAndAudioLookup() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/music/favorites.m3u");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("favorites.m3u")).thenReturn(true);
        when(storagePort.getFileSize(filePathStr)).thenReturn(512L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.asset().getFileType()).isEqualTo(FileType.PLAYLIST);
        assertThat(result.assetExif()).isNull();
        assertThat(result.assetAudio()).isNull();
        assertThat(result.thumbnailData()).isNotEmpty();
    }

    @Test
    void process_imageFile_exifReadThrows_returnsNullExifAndDoesNotPropagate() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/photos/broken.jpg");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("broken.jpg")).thenReturn(false);
        when(storagePort.isAudioFile("broken.jpg")).thenReturn(false);
        when(storagePort.isVideoFile("broken.jpg")).thenReturn(false);
        when(storagePort.getFileSize(filePathStr)).thenReturn(1024L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getImageRotation(filePathStr)).thenReturn(ImageRotation.ROTATE_0);
        when(storagePort.generateThumbnail(anyString(), anyInt(), anyInt())).thenReturn(new byte[]{1});
        when(storagePort.getExifMetadata(filePathStr)).thenThrow(new RuntimeException("corrupt exif"));

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.assetExif()).isNull();
    }

    @Test
    void process_audioFileWithValidAlbumArt_resizesArtworkToThumbnail() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/music/song.mp3");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("song.mp3")).thenReturn(false);
        when(storagePort.isAudioFile("song.mp3")).thenReturn(true);
        when(storagePort.getFileSize(filePathStr)).thenReturn(2048L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(audioMetadataService.extractAlbumArt(any())).thenReturn(Optional.of(validJpegBytes()));
        when(audioMetadataService.extract(any())).thenReturn(
                new AudioMetadata("Song", "Artist", "Album", 200, 320, 44100));

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.asset().getFileType()).isEqualTo(FileType.AUDIO);
        assertThat(result.thumbnailData()).isNotEmpty();
    }

    @Test
    void process_audioFileWithUndecodableAlbumArt_fallsBackToPlaceholder() throws Exception {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/music/song2.mp3");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("song2.mp3")).thenReturn(false);
        when(storagePort.isAudioFile("song2.mp3")).thenReturn(true);
        when(storagePort.getFileSize(filePathStr)).thenReturn(2048L);
        when(storagePort.computeHash(filePathStr)).thenReturn("hash");
        when(storagePort.getFileCreationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(storagePort.getFileModificationDateTime(filePathStr)).thenReturn(LocalDateTime.now());
        when(audioMetadataService.extractAlbumArt(any())).thenReturn(Optional.of(new byte[]{0, 1, 2, 3}));
        when(audioMetadataService.extract(any())).thenReturn(
                new AudioMetadata(null, null, null, null, null, null));

        CatalogBatchItem result = sut.process(filePath);

        assertThat(result.thumbnailData()).isNotEmpty();
    }

    @Test
    void process_ioExceptionFromStoragePort_propagatesAndIsLogged() {
        sut = new CatalogAssetItemProcessor(storagePort, audioMetadataService);
        Path filePath = Paths.get("/photos/error.jpg");
        String filePathStr = filePath.toAbsolutePath().toString();

        when(storagePort.isPlaylistFile("error.jpg")).thenReturn(false);
        when(storagePort.isAudioFile("error.jpg")).thenReturn(false);
        when(storagePort.getFileSize(filePathStr)).thenThrow(new RuntimeException("disk error"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.process(filePath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("disk error");
    }

    private byte[] validJpegBytes() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
