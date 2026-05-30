package com.jpablodrexler.photomanager.infrastructure.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbnailStorageHealthIndicatorTest {

    @Test
    void health_writableDirectory_returnsUp(@TempDir Path tempDir) {
        ThumbnailStorageHealthIndicator sut = new ThumbnailStorageHealthIndicator(tempDir.toString());

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void health_nonExistentPath_returnsDown() {
        ThumbnailStorageHealthIndicator sut = new ThumbnailStorageHealthIndicator("/tmp/does_not_exist_xzy123");

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void health_nonWritableDirectory_returnsDown(@TempDir Path tempDir) throws IOException {
        Set<PosixFilePermission> readOnly = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
        );
        try {
            Files.setPosixFilePermissions(tempDir, readOnly);
            ThumbnailStorageHealthIndicator sut = new ThumbnailStorageHealthIndicator(tempDir.toString());

            Health result = sut.health();

            assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        } finally {
            // restore so JUnit can clean up the temp dir
            Files.setPosixFilePermissions(tempDir, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        }
    }
}
