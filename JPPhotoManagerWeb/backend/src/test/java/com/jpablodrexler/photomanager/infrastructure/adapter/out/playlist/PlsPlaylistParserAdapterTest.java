package com.jpablodrexler.photomanager.infrastructure.adapter.out.playlist;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlsPlaylistParserAdapterTest {

    @Mock AssetRepository assetRepository;
    @InjectMocks PlsPlaylistParserAdapter sut;

    @TempDir
    Path tempDir;

    @Test
    void parse_validPlsFile_returnsAssetsInOrder() throws Exception {
        Asset track1 = Asset.builder().assetId(1L).fileName("track1.mp3").build();
        Asset track2 = Asset.builder().assetId(2L).fileName("track2.mp3").build();

        when(assetRepository.findByFileName("track1.mp3")).thenReturn(List.of(track1));
        when(assetRepository.findByFileName("track2.mp3")).thenReturn(List.of(track2));

        Path playlist = tempDir.resolve("playlist.pls");
        Files.writeString(playlist,
                "[playlist]\n" +
                "File1=/music/track1.mp3\n" +
                "Title1=Track 1\n" +
                "Length1=180\n" +
                "File2=/music/track2.mp3\n" +
                "Title2=Track 2\n" +
                "Length2=200\n" +
                "NumberOfEntries=2\n" +
                "Version=2\n");

        List<Asset> result = sut.parse(playlist);

        assertThat(result).extracting(Asset::getAssetId)
                .containsExactly(1L, 2L);
    }

    @Test
    void parse_outOfOrderEntries_returnsInNumericalOrder() throws Exception {
        Asset track1 = Asset.builder().assetId(1L).fileName("track1.mp3").build();
        Asset track2 = Asset.builder().assetId(2L).fileName("track2.mp3").build();

        when(assetRepository.findByFileName("track1.mp3")).thenReturn(List.of(track1));
        when(assetRepository.findByFileName("track2.mp3")).thenReturn(List.of(track2));

        Path playlist = tempDir.resolve("playlist.pls");
        Files.writeString(playlist,
                "[playlist]\n" +
                "File2=/music/track2.mp3\n" +
                "File1=/music/track1.mp3\n");

        List<Asset> result = sut.parse(playlist);

        assertThat(result).extracting(Asset::getAssetId)
                .containsExactly(1L, 2L);
    }
}
