package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.images.Artwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AudioMetadataServiceTest {

    @InjectMocks
    AudioMetadataService sut;

    @Test
    void extract_fileNotReadable_returnsEmptyMetadata(@TempDir Path tempDir) throws IOException {
        Path notAudio = tempDir.resolve("fake.mp3");
        Files.write(notAudio, new byte[]{0, 1, 2, 3});

        AudioMetadata result = sut.extract(notAudio);

        assertThat(result).isNotNull();
        assertThat(result.title()).isNull();
        assertThat(result.artist()).isNull();
        assertThat(result.album()).isNull();
    }

    @Test
    void extractAlbumArt_fileNotReadable_returnsEmpty(@TempDir Path tempDir) throws IOException {
        Path notAudio = tempDir.resolve("noart.mp3");
        Files.write(notAudio, new byte[]{0, 1, 2, 3});

        Optional<byte[]> result = sut.extractAlbumArt(notAudio);

        assertThat(result).isEmpty();
    }

    @Test
    void extractAlbumArt_missingFile_returnsEmpty() {
        Path missing = Path.of("/tmp/does_not_exist_12345.mp3");

        Optional<byte[]> result = sut.extractAlbumArt(missing);

        assertThat(result).isEmpty();
    }

    @Test
    void extract_missingFile_returnsEmptyMetadata() {
        Path missing = Path.of("/tmp/does_not_exist_12345.mp3");

        AudioMetadata result = sut.extract(missing);

        assertThat(result).isNotNull();
        assertThat(result.title()).isNull();
        assertThat(result.durationSeconds()).isNull();
    }
}
