package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.service.ExifMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplExifTest {

    @InjectMocks
    StorageServiceImpl sut;

    @TempDir
    Path tempDir;

    @Test
    void getExifMetadata_jpegWithExif_returnsCameraMakeAndModel() throws IOException {
        Path fixture = copyFixture("fixtures/test-with-exif.jpg", "test-with-exif.jpg");

        ExifMetadata result = sut.getExifMetadata(fixture.toString());

        assertThat(result).isNotNull();
        assertThat(result.cameraMake()).isEqualTo("TestCamera");
        assertThat(result.cameraModel()).isEqualTo("ModelX");
    }

    @Test
    void getExifMetadata_pngFile_returnsAllNullFields() throws IOException {
        Path pngFile = createPngFile();

        ExifMetadata result = sut.getExifMetadata(pngFile.toString());

        assertThat(result).isNotNull();
        assertThat(result.cameraMake()).isNull();
        assertThat(result.cameraModel()).isNull();
        assertThat(result.dateTaken()).isNull();
        assertThat(result.fNumber()).isNull();
        assertThat(result.isoSpeed()).isNull();
        assertThat(result.focalLength()).isNull();
        assertThat(result.gpsLatitude()).isNull();
        assertThat(result.gpsLongitude()).isNull();
    }

    @Test
    void getExifMetadata_nonExistentFile_returnsAllNullRecord() {
        ExifMetadata result = sut.getExifMetadata(tempDir.resolve("nonexistent.jpg").toString());

        assertThat(result).isNotNull();
        assertThat(result.cameraMake()).isNull();
        assertThat(result.cameraModel()).isNull();
    }

    private Path copyFixture(String resourcePath, String targetName) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(in).as("Fixture resource not found: " + resourcePath).isNotNull();
            Path target = tempDir.resolve(targetName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    private Path createPngFile() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Path pngPath = tempDir.resolve("test.png");
        ImageIO.write(img, "PNG", pngPath.toFile());
        return pngPath;
    }
}
