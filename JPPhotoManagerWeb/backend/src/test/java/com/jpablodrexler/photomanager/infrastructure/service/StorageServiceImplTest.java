package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceImplTest {

    StorageServiceAdapter sut;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sut = new StorageServiceAdapter(new SimpleMeterRegistry());
    }

    // --- listFiles ---

    @Test
    void listFiles_directoryWithImages_returnsOnlyImagePaths() throws IOException {
        Files.createFile(tempDir.resolve("photo.jpg"));
        Files.createFile(tempDir.resolve("image.png"));

        List<String> result = sut.listFiles(tempDir.toString());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.endsWith(".jpg") || p.endsWith(".png"));
    }

    @Test
    void listFiles_directoryWithMixedFiles_filtersOutNonImages() throws IOException {
        Files.createFile(tempDir.resolve("photo.jpg"));
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createFile(tempDir.resolve("data.xml"));

        List<String> result = sut.listFiles(tempDir.toString());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).endsWith("photo.jpg");
    }

    @Test
    void listFiles_nonExistentDirectory_returnsEmptyList() {
        List<String> result = sut.listFiles(tempDir.resolve("nonexistent").toString());

        assertThat(result).isEmpty();
    }

    // --- listSubDirectories ---

    @Test
    void listSubDirectories_directoryWithSubDirs_returnsSubDirPaths() throws IOException {
        Files.createDirectory(tempDir.resolve("sub1"));
        Files.createDirectory(tempDir.resolve("sub2"));
        Files.createFile(tempDir.resolve("file.jpg"));

        List<String> result = sut.listSubDirectories(tempDir.toString());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.endsWith("sub1") || p.endsWith("sub2"));
    }

    @Test
    void listSubDirectories_emptyDirectory_returnsEmptyList() {
        List<String> result = sut.listSubDirectories(tempDir.toString());

        assertThat(result).isEmpty();
    }

    @Test
    void listSubDirectories_nonExistentDirectory_returnsEmptyList() {
        List<String> result = sut.listSubDirectories(tempDir.resolve("nonexistent").toString());

        assertThat(result).isEmpty();
    }

    // --- directoryExists ---

    @Test
    void directoryExists_existingDirectory_returnsTrue() {
        boolean result = sut.directoryExists(tempDir.toString());

        assertThat(result).isTrue();
    }

    @Test
    void directoryExists_nonExistentPath_returnsFalse() {
        boolean result = sut.directoryExists(tempDir.resolve("nonexistent").toString());

        assertThat(result).isFalse();
    }

    @Test
    void directoryExists_regularFilePath_returnsFalse() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);

        boolean result = sut.directoryExists(file.toString());

        assertThat(result).isFalse();
    }

    // --- createDirectory ---

    @Test
    void createDirectory_newPath_createsDirectory() {
        String newDir = tempDir.resolve("newdir").toString();

        sut.createDirectory(newDir);

        assertThat(Path.of(newDir)).isDirectory();
    }

    @Test
    void createDirectory_nestedPath_createsAllIntermediateDirectories() {
        String nested = tempDir.resolve("a").resolve("b").resolve("c").toString();

        sut.createDirectory(nested);

        assertThat(Path.of(nested)).isDirectory();
    }

    // --- readFileBytes ---

    @Test
    void readFileBytes_existingFile_returnsCorrectBytes() throws IOException {
        byte[] expected = "hello world".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("test.bin");
        Files.write(file, expected);

        byte[] result = sut.readFileBytes(file.toString());

        assertThat(result).isEqualTo(expected);
    }

    // --- copyFile ---

    @Test
    void copyFile_existingFile_copiesContentToDestination() throws IOException {
        Path source = tempDir.resolve("source.jpg");
        Files.write(source, "content".getBytes(StandardCharsets.UTF_8));
        Path dest = tempDir.resolve("dest.jpg");

        sut.copyFile(source.toString(), dest.toString());

        assertThat(Files.readAllBytes(dest)).isEqualTo(Files.readAllBytes(source));
    }

    @Test
    void copyFile_existingDestination_replacesContent() throws IOException {
        Path source = tempDir.resolve("source.jpg");
        Files.write(source, "new content".getBytes(StandardCharsets.UTF_8));
        Path dest = tempDir.resolve("dest.jpg");
        Files.write(dest, "old content".getBytes(StandardCharsets.UTF_8));

        sut.copyFile(source.toString(), dest.toString());

        assertThat(new String(Files.readAllBytes(dest), StandardCharsets.UTF_8)).isEqualTo("new content");
    }

    // --- moveFile ---

    @Test
    void moveFile_existingFile_createsDestinationAndDeletesSource() throws IOException {
        Path source = tempDir.resolve("source.jpg");
        Files.write(source, "content".getBytes(StandardCharsets.UTF_8));
        Path dest = tempDir.resolve("dest.jpg");

        sut.moveFile(source.toString(), dest.toString());

        assertThat(dest).exists();
        assertThat(source).doesNotExist();
    }

    // --- deleteFile ---

    @Test
    void deleteFile_existingFile_removesIt() throws IOException {
        Path file = tempDir.resolve("delete-me.jpg");
        Files.createFile(file);

        sut.deleteFile(file.toString());

        assertThat(file).doesNotExist();
    }

    @Test
    void deleteFile_nonExistentFile_doesNotThrow() {
        assertThatCode(() -> sut.deleteFile(tempDir.resolve("missing.jpg").toString()))
                .doesNotThrowAnyException();
    }

    // --- computeHash ---

    @Test
    void computeHash_existingFile_returns64CharLowercaseHex() throws IOException {
        Path file = tempDir.resolve("photo.jpg");
        Files.write(file, "content".getBytes(StandardCharsets.UTF_8));

        String hash = sut.computeHash(file.toString());

        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void computeHash_nonExistentFile_throwsIOException() {
        assertThatThrownBy(() -> sut.computeHash(tempDir.resolve("missing.jpg").toString()))
                .isInstanceOf(IOException.class);
    }

    // --- getFileSize ---

    @Test
    void getFileSize_existingFile_returnsCorrectSize() throws IOException {
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("photo.jpg");
        Files.write(file, content);

        long size = sut.getFileSize(file.toString());

        assertThat(size).isEqualTo(content.length);
    }

    @Test
    void getFileSize_nonExistentFile_returnsZero() {
        long size = sut.getFileSize(tempDir.resolve("missing.jpg").toString());

        assertThat(size).isZero();
    }

    // --- getFileCreationDateTime / getFileModificationDateTime ---

    @Test
    void getFileCreationDateTime_existingFile_returnsNonNull() throws IOException {
        Path file = tempDir.resolve("photo.jpg");
        Files.createFile(file);

        LocalDateTime result = sut.getFileCreationDateTime(file.toString());

        assertThat(result).isNotNull();
    }

    @Test
    void getFileModificationDateTime_existingFile_returnsNonNull() throws IOException {
        Path file = tempDir.resolve("photo.jpg");
        Files.createFile(file);

        LocalDateTime result = sut.getFileModificationDateTime(file.toString());

        assertThat(result).isNotNull();
    }

    // --- loadImage ---

    @Test
    void loadImage_validPngFile_returnsBufferedImageWithCorrectDimensions() throws IOException {
        Path png = writePng("test.png", 20, 15);

        BufferedImage result = sut.loadImage(png.toString());

        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(20);
        assertThat(result.getHeight()).isEqualTo(15);
    }

    @Test
    void loadImage_nonExistentFile_throwsIOException() {
        assertThatThrownBy(() -> sut.loadImage(tempDir.resolve("missing.png").toString()))
                .isInstanceOf(IOException.class);
    }

    // --- getImageRotation ---

    @Test
    void getImageRotation_pngFileWithNoExif_returnsRotate0() throws IOException {
        Path png = writePng("test.png", 10, 10);

        ImageRotation result = sut.getImageRotation(png.toString());

        assertThat(result).isEqualTo(ImageRotation.ROTATE_0);
    }

    // --- generateThumbnail (BufferedImage overload) ---

    @Test
    void generateThumbnail_imageWithNoRotation_returnsNonEmptyJpegBytes() throws IOException {
        BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(image, 100, 75, ImageRotation.ROTATE_0);

        assertThat(result).isNotEmpty();
    }

    @Test
    void generateThumbnail_imageWithRotation90_swapsDimensionsAndReturnsBytes() throws IOException {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(image, 200, 150, ImageRotation.ROTATE_90);

        assertThat(result).isNotEmpty();
    }

    @Test
    void generateThumbnail_imageWithRotation180_returnsNonEmptyBytes() throws IOException {
        BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(image, 100, 75, ImageRotation.ROTATE_180);

        assertThat(result).isNotEmpty();
    }

    @Test
    void generateThumbnail_imageWithRotation270_returnsNonEmptyBytes() throws IOException {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);

        byte[] result = sut.generateThumbnail(image, 200, 150, ImageRotation.ROTATE_270);

        assertThat(result).isNotEmpty();
    }

    // --- generateThumbnail (file path overload) ---

    @Test
    void generateThumbnail_validPngFile_returnsNonEmptyJpegBytes() throws IOException {
        Path png = writePng("thumb.png", 200, 150);

        byte[] result = sut.generateThumbnail(png.toString(), 100, 75);

        assertThat(result).isNotEmpty();
    }

    @Test
    void generateThumbnail_validPngFile_timerRecordsNonZeroDuration() throws IOException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        StorageServiceAdapter adapterWithRegistry = new StorageServiceAdapter(registry);
        Path png = writePng("timed.png", 100, 75);

        adapterWithRegistry.generateThumbnail(png.toString(), 50, 40);

        Timer timer = registry.find("photomanager_thumbnail_generation_seconds").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isGreaterThan(0);
    }

    // --- helper ---

    private Path writePng(String name, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Path file = tempDir.resolve(name);
        ImageIO.write(img, "PNG", file.toFile());
        return file;
    }
}
