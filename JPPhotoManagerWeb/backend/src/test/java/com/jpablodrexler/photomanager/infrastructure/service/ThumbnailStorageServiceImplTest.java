package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class ThumbnailStorageServiceImplTest {

    @InjectMocks
    ThumbnailStorageServiceAdapter sut;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "thumbnailsDirectory", tempDir.toString());
    }

    @Test
    void saveThumbnail_validData_writesCorrectBytesToDisk() throws Exception {
        byte[] data = new byte[]{10, 20, 30};

        sut.saveThumbnail("1.bin", data);

        assertThat(Files.readAllBytes(tempDir.resolve("1.bin"))).isEqualTo(data);
    }

    @Test
    void loadThumbnail_existingBlob_returnsBytes() {
        byte[] data = new byte[]{7, 8, 9};
        sut.saveThumbnail("2.bin", data);

        byte[] result = sut.loadThumbnail("2.bin");

        assertThat(result).isEqualTo(data);
    }

    @Test
    void loadThumbnail_missingBlob_returnsNull() {
        byte[] result = sut.loadThumbnail("missing.bin");

        assertThat(result).isNull();
    }

    @Test
    void deleteThumbnail_existingBlob_removesFileFromDisk() {
        sut.saveThumbnail("3.bin", new byte[]{1});

        sut.deleteThumbnail("3.bin");

        assertThat(tempDir.resolve("3.bin")).doesNotExist();
    }

    @Test
    void deleteThumbnail_missingBlob_doesNotThrow() {
        assertThatCode(() -> sut.deleteThumbnail("nonexistent.bin"))
                .doesNotThrowAnyException();
    }

    @Test
    void thumbnailExists_existingBlob_returnsTrue() {
        sut.saveThumbnail("4.bin", new byte[]{1});

        boolean result = sut.thumbnailExists("4.bin");

        assertThat(result).isTrue();
    }

    @Test
    void thumbnailExists_missingBlob_returnsFalse() {
        boolean result = sut.thumbnailExists("missing.bin");

        assertThat(result).isFalse();
    }
}
