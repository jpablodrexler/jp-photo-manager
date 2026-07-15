package com.jpablodrexler.photomanager.infrastructure.service;

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
class M3uPlaylistParserAdapterTest {

    @Mock AssetRepository assetRepository;
    @InjectMocks M3uPlaylistParserAdapter sut;

    @TempDir
    Path tempDir;

    @Test
    void parse_validM3uFile_returnsAssetsInOrder() throws Exception {
        Asset track1 = Asset.builder().assetId(1L).fileName("track1.mp3").build();
        Asset track2 = Asset.builder().assetId(2L).fileName("track2.mp3").build();
        Asset track3 = Asset.builder().assetId(3L).fileName("track3.mp3").build();

        when(assetRepository.findByFileName("track1.mp3")).thenReturn(List.of(track1));
        when(assetRepository.findByFileName("track2.mp3")).thenReturn(List.of(track2));
        when(assetRepository.findByFileName("track3.mp3")).thenReturn(List.of(track3));

        Path playlist = tempDir.resolve("playlist.m3u");
        Files.writeString(playlist,
                "#EXTM3U\n" +
                "#EXTINF:180,Artist1 - Track1\n" +
                "/music/artist1/track1.mp3\n" +
                "#EXTINF:200,Artist2 - Track2\n" +
                "/music/artist2/track2.mp3\n" +
                "#EXTINF:150,Artist3 - Track3\n" +
                "/music/artist3/track3.mp3\n");

        List<Asset> result = sut.parse(playlist);

        assertThat(result).extracting(Asset::getAssetId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void parse_fileNotInCatalog_skipsEntry() throws Exception {
        Asset track1 = Asset.builder().assetId(1L).fileName("track1.mp3").build();
        when(assetRepository.findByFileName("track1.mp3")).thenReturn(List.of(track1));
        when(assetRepository.findByFileName("unknown.mp3")).thenReturn(List.of());

        Path playlist = tempDir.resolve("playlist.m3u");
        Files.writeString(playlist,
                "#EXTM3U\n" +
                "/music/track1.mp3\n" +
                "/music/unknown.mp3\n");

        List<Asset> result = sut.parse(playlist);

        assertThat(result).extracting(Asset::getAssetId).containsExactly(1L);
    }
}
