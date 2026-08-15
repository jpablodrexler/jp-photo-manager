package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAlbumSummaryUseCaseImplTest {

    @Mock
    AlbumRepository albumRepository;
    @Mock
    AlbumAssetFilterFactory albumAssetFilterFactory;
    @InjectMocks
    GetAlbumSummaryUseCaseImpl sut;

    @Test
    void execute_albumNotFound_throwsAlbumNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(albumRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute(99L, userId))
                .isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    void execute_albumWithoutFilterJson_returnsAlbumDataUsingCountAssets() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Album album = Album.builder()
                .albumId(1L)
                .userId(userId)
                .name("Trip")
                .description("desc")
                .createdAt(createdAt)
                .filterJson(null)
                .build();
        when(albumRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(album));
        when(albumRepository.countAssets(1L)).thenReturn(5L);

        AlbumData result = sut.execute(1L, userId);

        assertThat(result).isEqualTo(new AlbumData(1L, "Trip", "desc", createdAt, 5L, null));
        verify(albumAssetFilterFactory, never()).build(any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void execute_albumWithFilterJson_returnsAlbumDataUsingSmartAlbumCount() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        String filterJson = "{\"search\":\"sunset\"}";
        Album album = Album.builder()
                .albumId(2L)
                .userId(userId)
                .name("Smart")
                .description(null)
                .createdAt(createdAt)
                .filterJson(filterJson)
                .build();
        AssetFilter builtFilter = new AssetFilter(null, "sunset", null, null, null, null, 0, 1, false, java.util.Set.of());
        when(albumRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(album));
        when(albumAssetFilterFactory.build(filterJson, 0, 1)).thenReturn(builtFilter);
        when(albumRepository.findSmartAlbumAssets(builtFilter, 0, 1))
                .thenReturn(new PaginatedResult<>(List.of(), 42L, 0, 1));

        AlbumData result = sut.execute(2L, userId);

        assertThat(result.assetCount()).isEqualTo(42L);
        assertThat(result.filterJson()).isEqualTo(filterJson);
        verify(albumRepository, never()).countAssets(any());
    }
}
