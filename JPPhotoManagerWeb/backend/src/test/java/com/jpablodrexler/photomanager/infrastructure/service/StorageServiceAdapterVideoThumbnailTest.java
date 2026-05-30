package com.jpablodrexler.photomanager.infrastructure.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceAdapterVideoThumbnailTest {

    StorageServiceAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new StorageServiceAdapter(new SimpleMeterRegistry());
        Assumptions.assumeTrue(ffmpegAvailable(), "Skipping: ffmpeg not installed");
    }

    @Test
    void generateThumbnail_validMp4_returnsBytesStartingWithJpegMagic() throws IOException {
        URL resource = getClass().getClassLoader().getResource("test-video.mp4");
        Assumptions.assumeTrue(resource != null, "Skipping: test-video.mp4 not found in test resources");
        Path videoPath = Paths.get(resource.getPath());

        byte[] bytes = sut.generateThumbnail(videoPath.toString(), 200, 150);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 0xFF);
        assertThat(bytes[1]).isEqualTo((byte) 0xD8);
        assertThat(bytes[2]).isEqualTo((byte) 0xFF);
    }

    private boolean ffmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
