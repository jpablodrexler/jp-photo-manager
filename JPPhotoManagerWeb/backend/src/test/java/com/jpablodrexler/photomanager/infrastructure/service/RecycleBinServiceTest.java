package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecycleBinServiceTest {

    @Mock
    AssetRepository assetRepository;

    @Mock
    StorageService storageService;

    @Mock
    ThumbnailStorageService thumbnailStorageService;

    @InjectMocks
    RecycleBinServiceImpl sut;

    @Test
    void getDeletedAssets_returnsPagedResults() {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");
        asset.setDeletedAt(LocalDateTime.now().minusDays(1));
        when(assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset)));

        PaginatedData<Asset> result = sut.getDeletedAssets(0);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getAssetId()).isEqualTo(1L);
    }

    @Test
    void restoreAssets_setsDeletedAtToNull() {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");
        asset.setDeletedAt(LocalDateTime.now().minusDays(5));
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        sut.restoreAssets(List.of(1L));

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNull();
    }

    @Test
    void purgeAssets_deletesFileAndThumbnailAndRow() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        sut.purgeAssets(List.of(1L));

        verify(storageService).deleteFile("/photos/photo.jpg");
        verify(thumbnailStorageService).deleteThumbnail("1.bin");
        verify(assetRepository).delete(asset);
    }

    @Test
    void purgeAssets_fileDeleteFails_stillDeletesThumbnailAndRow() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        doThrow(new IOException("gone")).when(storageService).deleteFile(any());

        sut.purgeAssets(List.of(1L));

        verify(thumbnailStorageService).deleteThumbnail("1.bin");
        verify(assetRepository).delete(asset);
    }

    @Test
    void purgeExpired_onlyPurgesAssetsOlderThanCutoff() throws IOException {
        Asset recentAsset = buildAsset(1L, "recent.jpg", "/photos");
        recentAsset.setDeletedAt(LocalDateTime.now().minusDays(1));

        Asset oldAsset = buildAsset(2L, "old.jpg", "/photos");
        oldAsset.setDeletedAt(LocalDateTime.now().minusDays(40));

        when(assetRepository.findByDeletedAtBeforeAndDeletedAtIsNotNull(any(LocalDateTime.class)))
                .thenReturn(List.of(oldAsset));
        when(assetRepository.findAllById(List.of(2L))).thenReturn(List.of(oldAsset));

        sut.purgeExpired(30);

        verify(assetRepository).delete(oldAsset);
        verify(assetRepository, never()).delete(recentAsset);
    }

    private Asset buildAsset(Long id, String fileName, String folderPath) {
        Folder folder = new Folder();
        folder.setPath(folderPath);
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFileName(fileName);
        asset.setFolder(folder);
        return asset;
    }
}
