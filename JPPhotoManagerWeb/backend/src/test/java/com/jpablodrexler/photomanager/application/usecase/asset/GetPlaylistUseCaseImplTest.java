package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.playlist.PlaylistParserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlaylistUseCaseImplTest {

    @Mock
    AssetRepository assetRepository;
    @Mock
    PlaylistParserPort m3uParser;
    @Mock
    PlaylistParserPort plsParser;

    @Test
    void execute_assetNotFound_throwsNoSuchElementException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());
        GetPlaylistUseCaseImpl sut = new GetPlaylistUseCaseImpl(assetRepository, List.of(m3uParser));

        assertThatThrownBy(() -> sut.execute(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("1");
    }

    @Test
    void execute_noParserSupportsFile_throwsIllegalArgumentException() {
        Asset asset = Asset.builder().assetId(1L).fileName("mix.xyz").build();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(m3uParser.supports("mix.xyz")).thenReturn(false);
        GetPlaylistUseCaseImpl sut = new GetPlaylistUseCaseImpl(assetRepository, List.of(m3uParser));

        assertThatThrownBy(() -> sut.execute(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mix.xyz");
    }

    @Test
    void execute_supportingParserFound_returnsParsedAssets() {
        Asset asset = Asset.builder().assetId(1L).fileName("mix.m3u").build();
        Asset parsedAsset = Asset.builder().assetId(2L).fileName("song.mp3").build();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(m3uParser.supports("mix.m3u")).thenReturn(true);
        when(m3uParser.parse(Path.of("mix.m3u"))).thenReturn(List.of(parsedAsset));
        GetPlaylistUseCaseImpl sut = new GetPlaylistUseCaseImpl(assetRepository, List.of(m3uParser));

        List<Asset> result = sut.execute(1L);

        assertThat(result).containsExactly(parsedAsset);
    }

    @Test
    void execute_multipleParsers_usesFirstOneThatSupportsTheFile() {
        Asset asset = Asset.builder().assetId(1L).fileName("mix.pls").build();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(m3uParser.supports("mix.pls")).thenReturn(false);
        when(plsParser.supports("mix.pls")).thenReturn(true);
        when(plsParser.parse(Path.of("mix.pls"))).thenReturn(List.of());
        GetPlaylistUseCaseImpl sut = new GetPlaylistUseCaseImpl(assetRepository, List.of(m3uParser, plsParser));

        sut.execute(1L);

        verify(plsParser).parse(Path.of("mix.pls"));
        verify(m3uParser, never()).parse(org.mockito.ArgumentMatchers.any());
    }
}
