package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceAdapterTest {

    private final StorageServiceAdapter sut = new StorageServiceAdapter(new SimpleMeterRegistry());

    // --- listFiles / listSubDirectories ---

    @Test
    void listFiles_mixedDirectory_returnsOnlyMediaFilesSorted(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.jpg"), "x");
        Files.writeString(dir.resolve("a.mp3"), "x");
        Files.writeString(dir.resolve("notes.txt"), "x");
        Files.createDirectory(dir.resolve("subdir"));

        List<String> result = sut.listFiles(dir.toString());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).endsWith("a.mp3");
        assertThat(result.get(1)).endsWith("b.jpg");
    }

    @Test
    void listFiles_nonExistentDirectory_returnsEmptyList() {
        List<String> result = sut.listFiles("/does/not/exist/xyz");

        assertThat(result).isEmpty();
    }

    @Test
    void listSubDirectories_directoryWithSubfoldersAndFiles_returnsOnlySubfolders(@TempDir Path dir)
            throws IOException {
        Files.createDirectory(dir.resolve("sub1"));
        Files.createDirectory(dir.resolve("sub2"));
        Files.writeString(dir.resolve("file.jpg"), "x");

        List<String> result = sut.listSubDirectories(dir.toString());

        assertThat(result).hasSize(2);
    }

    @Test
    void listSubDirectories_nonExistentDirectory_returnsEmptyList() {
        List<String> result = sut.listSubDirectories("/does/not/exist/xyz");

        assertThat(result).isEmpty();
    }

    // --- directoryExists / fileExists ---

    @Test
    void directoryExists_existingDirectory_returnsTrue(@TempDir Path dir) {
        assertThat(sut.directoryExists(dir.toString())).isTrue();
    }

    @Test
    void directoryExists_nonExistentPath_returnsFalse() {
        assertThat(sut.directoryExists("/does/not/exist/xyz")).isFalse();
    }

    @Test
    void fileExists_existingFile_returnsTrue(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.jpg");
        Files.writeString(file, "x");

        assertThat(sut.fileExists(file.toString())).isTrue();
    }

    @Test
    void fileExists_nonExistentFile_returnsFalse(@TempDir Path dir) {
        assertThat(sut.fileExists(dir.resolve("missing.jpg").toString())).isFalse();
    }

    // --- createDirectory ---

    @Test
    void createDirectory_nestedPath_createsAllMissingDirectories(@TempDir Path dir) {
        Path nested = dir.resolve("a/b/c");

        sut.createDirectory(nested.toString());

        assertThat(Files.isDirectory(nested)).isTrue();
    }

    // --- readFileBytes / copyFile / moveFile / deleteFile ---

    @Test
    void readFileBytes_existingFile_returnsExactContent(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("data.bin");
        Files.write(file, new byte[] {1, 2, 3, 4});

        byte[] result = sut.readFileBytes(file.toString());

        assertThat(result).containsExactly(1, 2, 3, 4);
    }

    @Test
    void copyFile_nestedDestination_createsParentDirsAndCopiesContent(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("source.jpg");
        Files.writeString(source, "hello");
        Path destination = dir.resolve("nested/dest.jpg");

        sut.copyFile(source.toString(), destination.toString());

        assertThat(Files.readString(destination)).isEqualTo("hello");
        assertThat(Files.exists(source)).isTrue();
    }

    @Test
    void moveFile_nestedDestination_movesContentAndRemovesSource(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("source.jpg");
        Files.writeString(source, "hello");
        Path destination = dir.resolve("nested/dest.jpg");

        sut.moveFile(source.toString(), destination.toString());

        assertThat(Files.readString(destination)).isEqualTo("hello");
        assertThat(Files.exists(source)).isFalse();
    }

    @Test
    void deleteFile_existingFile_removesIt(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.jpg");
        Files.writeString(file, "x");

        sut.deleteFile(file.toString());

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void deleteFile_nonExistentFile_doesNotThrow(@TempDir Path dir) throws IOException {
        sut.deleteFile(dir.resolve("missing.jpg").toString());
    }

    // --- computeHash ---

    @Test
    void computeHash_knownContent_returnsExpectedSha256Hex(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("hash-me.txt");
        Files.writeString(file, "hello world");

        String result = sut.computeHash(file.toString());

        assertThat(result).isEqualTo(sha256Hex("hello world"));
    }

    private static String sha256Hex(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // --- getFileSize / getFileCreationDateTime / getFileModificationDateTime ---

    @Test
    void getFileSize_existingFile_returnsByteCount(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("data.bin");
        Files.write(file, new byte[10]);

        assertThat(sut.getFileSize(file.toString())).isEqualTo(10L);
    }

    @Test
    void getFileSize_nonExistentFile_returnsZero(@TempDir Path dir) {
        assertThat(sut.getFileSize(dir.resolve("missing.bin").toString())).isEqualTo(0L);
    }

    @Test
    void getFileCreationDateTime_existingFile_returnsRecentTimestamp(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.jpg");
        Files.writeString(file, "x");

        LocalDateTime result = sut.getFileCreationDateTime(file.toString());

        assertThat(result).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void getFileModificationDateTime_existingFile_returnsRecentTimestamp(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.jpg");
        Files.writeString(file, "x");

        LocalDateTime result = sut.getFileModificationDateTime(file.toString());

        assertThat(result).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
    }

    // --- isAudioFile / isPlaylistFile / isVideoFile ---

    @Test
    void isAudioFile_audioExtensions_returnsTrue() {
        assertThat(sut.isAudioFile("song.mp3")).isTrue();
        assertThat(sut.isAudioFile("song.FLAC")).isTrue();
        assertThat(sut.isAudioFile("song.wav")).isTrue();
    }

    @Test
    void isAudioFile_nonAudioExtension_returnsFalse() {
        assertThat(sut.isAudioFile("photo.jpg")).isFalse();
    }

    @Test
    void isPlaylistFile_playlistExtensions_returnsTrue() {
        assertThat(sut.isPlaylistFile("list.m3u")).isTrue();
        assertThat(sut.isPlaylistFile("list.M3U8")).isTrue();
        assertThat(sut.isPlaylistFile("list.pls")).isTrue();
    }

    @Test
    void isPlaylistFile_nonPlaylistExtension_returnsFalse() {
        assertThat(sut.isPlaylistFile("photo.jpg")).isFalse();
    }

    @Test
    void isVideoFile_videoExtensions_returnsTrue() {
        assertThat(sut.isVideoFile("clip.mp4")).isTrue();
        assertThat(sut.isVideoFile("clip.MOV")).isTrue();
        assertThat(sut.isVideoFile("clip.mkv")).isTrue();
    }

    @Test
    void isVideoFile_nonVideoExtension_returnsFalse() {
        assertThat(sut.isVideoFile("photo.jpg")).isFalse();
    }

    // --- getImageRotation ---

    @Test
    void getImageRotation_videoFile_returnsRotate0WithoutReadingMetadata() throws IOException {
        ImageRotation result = sut.getImageRotation("clip.mp4");

        assertThat(result).isEqualTo(ImageRotation.ROTATE_0);
    }

    @Test
    void getImageRotation_unreadableFile_defaultsToRotate0(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("not-really-a.jpg");
        Files.writeString(file, "this is not image data");

        ImageRotation result = sut.getImageRotation(file.toString());

        assertThat(result).isEqualTo(ImageRotation.ROTATE_0);
    }

    // --- getExifMetadata ---

    @Test
    void getExifMetadata_videoFile_returnsAllNullMetadata() {
        ExifMetadata result = sut.getExifMetadata("clip.mp4");

        assertThat(result.cameraMake()).isNull();
        assertThat(result.rawExif()).isNull();
        assertThat(result.dateTaken()).isNull();
    }

    @Test
    void getExifMetadata_unreadableFile_returnsAllNullMetadata(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("not-really-a.jpg");
        Files.writeString(file, "this is not image data");

        ExifMetadata result = sut.getExifMetadata(file.toString());

        assertThat(result.cameraMake()).isNull();
        assertThat(result.gpsLatitude()).isNull();
        assertThat(result.rawExif()).isNull();
    }

    // --- loadImage / generateThumbnail / convertPngToJpeg ---

    @Test
    void loadImage_validPngFile_returnsDecodedImageWithCorrectDimensions(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("source.png");
        writePng(file, 40, 30);

        BufferedImage result = sut.loadImage(file.toString());

        assertThat(result.getWidth()).isEqualTo(40);
        assertThat(result.getHeight()).isEqualTo(30);
    }

    @Test
    void loadImage_unsupportedContent_throwsIOException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bogus.jpg");
        Files.writeString(file, "not an image");

        assertThatThrownBy(() -> sut.loadImage(file.toString())).isInstanceOf(IOException.class);
    }

    @Test
    void generateThumbnail_bufferedImageNoRotation_scalesDownPreservingLandscapeAspect() throws IOException {
        BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(source, 200, 150, ImageRotation.ROTATE_0);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(200);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(150);
        assertThat(decoded.getWidth()).isGreaterThan(decoded.getHeight());
    }

    @Test
    void generateThumbnail_bufferedImageRotated90_swapsDimensionsBeforeScaling() throws IOException {
        BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(source, 200, 150, ImageRotation.ROTATE_90);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        // A 400x300 landscape image rotated 90 degrees becomes a 300x400 portrait image before
        // scaling, so the resulting thumbnail should be taller than it is wide.
        assertThat(decoded.getHeight()).isGreaterThan(decoded.getWidth());
    }

    @Test
    void generateThumbnail_fromFilePath_producesDecodableThumbnail(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("source.png");
        writePng(file, 400, 300);

        byte[] result = sut.generateThumbnail(file.toString(), 200, 150);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded).isNotNull();
    }

    @Test
    void generateThumbnail_videoFileWithoutFfmpegOrInvalidInput_throwsIOException(@TempDir Path dir) {
        Path fakeVideo = dir.resolve("clip.mp4");

        assertThatThrownBy(() -> sut.generateThumbnail(fakeVideo.toString(), 200, 150))
                .isInstanceOf(IOException.class);
    }

    @Test
    void convertPngToJpeg_validPng_writesReadableJpegWithSameDimensions(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("source.png");
        writePng(source, 50, 25);
        Path destination = dir.resolve("dest.jpg");

        sut.convertPngToJpeg(source.toString(), destination.toString());

        BufferedImage jpeg = ImageIO.read(destination.toFile());
        assertThat(jpeg.getWidth()).isEqualTo(50);
        assertThat(jpeg.getHeight()).isEqualTo(25);
    }

    private static void writePng(Path file, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "PNG", file.toFile());
    }
}
