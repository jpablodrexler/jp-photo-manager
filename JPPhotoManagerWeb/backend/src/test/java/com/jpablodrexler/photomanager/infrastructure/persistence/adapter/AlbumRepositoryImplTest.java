package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AlbumEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAlbumRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AlbumEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumRepositoryImplTest {

    @Mock JpaAlbumRepository jpa;
    @Mock AlbumEntityMapper albumMapper;
    @Mock AssetEntityMapper assetMapper;
    @InjectMocks AlbumRepositoryImpl sut;

    @Test
    void findByUserId_returnsMappedAlbums() {
        UUID userId = UUID.randomUUID();
        AlbumEntity entity = new AlbumEntity();
        Album domain = Album.builder().albumId(1L).name("Vacation").build();
        when(jpa.findByUser_Id(userId)).thenReturn(List.of(entity));
        when(albumMapper.toDomain(entity)).thenReturn(domain);

        List<Album> result = sut.findByUserId(userId);

        assertThat(result).containsExactly(domain);
    }

    @Test
    void findByIdAndUserId_present_returnsMappedAlbum() {
        UUID userId = UUID.randomUUID();
        AlbumEntity entity = new AlbumEntity();
        Album domain = Album.builder().albumId(1L).build();
        when(jpa.findByAlbumIdAndUser_Id(1L, userId)).thenReturn(Optional.of(entity));
        when(albumMapper.toDomain(entity)).thenReturn(domain);

        Optional<Album> result = sut.findByIdAndUserId(1L, userId);

        assertThat(result).contains(domain);
    }

    @Test
    void findByIdAndUserId_absent_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(jpa.findByAlbumIdAndUser_Id(99L, userId)).thenReturn(Optional.empty());

        assertThat(sut.findByIdAndUserId(99L, userId)).isEmpty();
    }

    @Test
    void save_mapsAndPersists() {
        Album album = Album.builder().albumId(1L).name("Test").build();
        AlbumEntity entity = new AlbumEntity();
        when(albumMapper.toEntity(album)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(albumMapper.toDomain(entity)).thenReturn(album);

        Album result = sut.save(album);

        assertThat(result).isEqualTo(album);
    }

    @Test
    void deleteById_delegatesToJpa() {
        sut.deleteById(5L);
        verify(jpa).deleteById(5L);
    }

    @Test
    void findAssetsByAlbumId_returnsPaginatedResult() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(1L).build();
        Page<AssetEntity> page = new PageImpl<>(List.of(entity));
        when(jpa.findAssetsByAlbumId(eq(1L), any())).thenReturn(page);
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        PaginatedResult<Asset> result = sut.findAssetsByAlbumId(1L, 0, 50);

        assertThat(result.items()).containsExactly(domain);
        assertThat(result.pageSize()).isEqualTo(50);
    }

    @Test
    void findAssetsByAlbumId_withZeroPageSize_usesDefaultPageSize() {
        Page<AssetEntity> page = new PageImpl<>(List.of());
        when(jpa.findAssetsByAlbumId(eq(1L), any())).thenReturn(page);

        PaginatedResult<Asset> result = sut.findAssetsByAlbumId(1L, 0, 0);

        assertThat(result.pageSize()).isEqualTo(100);
    }

    @Test
    void countAssets_delegatesToJpa() {
        when(jpa.countAssets(1L)).thenReturn(25L);

        assertThat(sut.countAssets(1L)).isEqualTo(25L);
    }

    @Test
    void addAssets_callsAddAssetForEachId() {
        sut.addAssets(1L, List.of(10L, 20L));

        verify(jpa).addAsset(1L, 10L);
        verify(jpa).addAsset(1L, 20L);
    }

    @Test
    void removeAssets_callsRemoveAssetForEachId() {
        sut.removeAssets(1L, List.of(10L, 20L));

        verify(jpa).removeAsset(1L, 10L);
        verify(jpa).removeAsset(1L, 20L);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
