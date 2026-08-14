package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @Test
    void extract_validAudioFileWithTags_returnsPopulatedMetadata(@TempDir Path tempDir) throws Exception {
        Path wav = writeSilentWav(tempDir, "tagged.wav");
        writeTags(wav, "My Title", "My Artist", "My Album");

        AudioMetadata result = sut.extract(wav);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("My Title");
        assertThat(result.artist()).isEqualTo("My Artist");
        assertThat(result.album()).isEqualTo("My Album");
        assertThat(result.durationSeconds()).isNotNull();
        assertThat(result.sampleRateHz()).isNotNull();
    }

    @Test
    void extract_validAudioFileWithoutTags_returnsHeaderInfoWithNullTagFields(@TempDir Path tempDir) throws Exception {
        Path wav = writeSilentWav(tempDir, "untagged.wav");

        AudioMetadata result = sut.extract(wav);

        assertThat(result).isNotNull();
        assertThat(result.durationSeconds()).isNotNull();
    }

    @Test
    void extractAlbumArt_taggedFileWithArtwork_returnsBinaryData(@TempDir Path tempDir) throws Exception {
        Path wav = writeSilentWav(tempDir, "withart.wav");
        byte[] artworkBytes = validPngBytes();

        AudioFile audioFile = AudioFileIO.read(wav.toFile());
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        Artwork artwork = ArtworkFactory.getNew();
        artwork.setBinaryData(artworkBytes);
        artwork.setMimeType("image/png");
        tag.setField(artwork);
        audioFile.setTag(tag);
        audioFile.commit();

        Optional<byte[]> result = sut.extractAlbumArt(wav);

        assertThat(result).isPresent();
        assertThat(result.get()).isNotEmpty();
    }

    @Test
    void extractAlbumArt_taggedFileWithoutArtwork_returnsEmpty(@TempDir Path tempDir) throws Exception {
        Path wav = writeSilentWav(tempDir, "noart.wav");
        writeTags(wav, "Title Only", null, null);

        Optional<byte[]> result = sut.extractAlbumArt(wav);

        assertThat(result).isEmpty();
    }

    private Path writeSilentWav(Path dir, String fileName) throws IOException {
        Path wav = dir.resolve(fileName);
        AudioFormat format = new AudioFormat(8000f, 8, 1, true, false);
        byte[] silence = new byte[800];
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(silence), format, silence.length)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wav.toFile());
        }
        return wav;
    }

    private void writeTags(Path wav, String title, String artist, String album) throws Exception {
        AudioFile audioFile = AudioFileIO.read(wav.toFile());
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        if (title != null) tag.setField(FieldKey.TITLE, title);
        if (artist != null) tag.setField(FieldKey.ARTIST, artist);
        if (album != null) tag.setField(FieldKey.ALBUM, album);
        audioFile.setTag(tag);
        audioFile.commit();
    }

    private byte[] validPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
