package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Slf4j
public class AudioMetadataService {

    static {
        // Silence jaudiotagger's own verbose java.util.logging output
        Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
    }

    public AudioMetadata extract(Path filePath) {
        try {
            AudioFile audioFile = AudioFileIO.read(filePath.toFile());
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            String title = readTagField(tag, FieldKey.TITLE);
            String artist = readTagField(tag, FieldKey.ARTIST);
            String album = readTagField(tag, FieldKey.ALBUM);
            Integer duration = header != null ? header.getTrackLength() : null;
            Integer bitrate = header != null ? safeLongToInt(header.getBitRateAsNumber()) : null;
            Integer sampleRate = header != null ? parseSampleRate(header.getSampleRate()) : null;

            return new AudioMetadata(title, artist, album, duration, bitrate, sampleRate);
        } catch (CannotReadException e) {
            log.warn("Cannot read audio metadata from {}: {}", filePath, e.getMessage());
            return new AudioMetadata(null, null, null, null, null, null);
        } catch (Exception e) {
            log.warn("Failed to extract audio metadata from {}", filePath, e);
            return new AudioMetadata(null, null, null, null, null, null);
        }
    }

    public Optional<byte[]> extractAlbumArt(Path filePath) {
        try {
            AudioFile audioFile = AudioFileIO.read(filePath.toFile());
            Tag tag = audioFile.getTag();
            if (tag == null) return Optional.empty();

            List<Artwork> artworkList = tag.getArtworkList();
            if (artworkList == null || artworkList.isEmpty()) return Optional.empty();

            byte[] data = artworkList.get(0).getBinaryData();
            if (data == null || data.length == 0) return Optional.empty();

            return Optional.of(data);
        } catch (CannotReadException e) {
            log.warn("Cannot read album art from {}: {}", filePath, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to extract album art from {}", filePath, e);
            return Optional.empty();
        }
    }

    private String readTagField(Tag tag, FieldKey key) {
        if (tag == null) return null;
        try {
            String value = tag.getFirst(key);
            return (value != null && !value.isBlank()) ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private Integer parseSampleRate(String sampleRate) {
        if (sampleRate == null || sampleRate.isBlank()) return null;
        try {
            return Integer.parseInt(sampleRate.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
