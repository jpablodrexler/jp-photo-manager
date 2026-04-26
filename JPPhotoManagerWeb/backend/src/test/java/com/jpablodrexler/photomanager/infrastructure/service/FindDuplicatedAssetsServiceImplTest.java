package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindDuplicatedAssetsServiceImplTest {

    @Mock
    AssetRepository assetRepository;

    @Mock
    StorageService storageService;

    @InjectMocks
    FindDuplicatedAssetsServiceImpl sut;

    @Test
    void getDuplicatedAssets_noAssets_returnsEmptyList() {
        when(assetRepository.findAll()).thenReturn(List.of());

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).isEmpty();
    }

    @Test
    void getDuplicatedAssets_allUniqueHashes_returnsEmptyList() {
        Asset assetA = buildAsset(1L, "a.jpg", "hash-aaa", "/photos");
        Asset assetB = buildAsset(2L, "b.jpg", "hash-bbb", "/photos");
        when(assetRepository.findAll()).thenReturn(List.of(assetA, assetB));
        when(storageService.directoryExists("/photos")).thenReturn(true);
        when(storageService.getFileSize("/photos/a.jpg")).thenReturn(1000L);
        when(storageService.getFileSize("/photos/b.jpg")).thenReturn(2000L);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).isEmpty();
    }

    @Test
    void getDuplicatedAssets_duplicatesFound_returnsGroupWithBothAssets() {
        Asset assetA = buildAsset(1L, "a.jpg", "same-hash", "/photos");
        Asset assetB = buildAsset(2L, "b.jpg", "same-hash", "/photos");
        when(assetRepository.findAll()).thenReturn(List.of(assetA, assetB));
        when(storageService.directoryExists("/photos")).thenReturn(true);
        when(storageService.getFileSize("/photos/a.jpg")).thenReturn(1000L);
        when(storageService.getFileSize("/photos/b.jpg")).thenReturn(1000L);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(2);
        assertThat(result.get(0)).containsExactlyInAnyOrder(assetA, assetB);
    }

    @Test
    void getDuplicatedAssets_staleAsset_filteredOutBeforeGrouping() {
        Asset validAsset = buildAsset(1L, "a.jpg", "same-hash", "/photos");
        Asset staleAsset = buildAsset(2L, "b.jpg", "same-hash", "/stale");
        when(assetRepository.findAll()).thenReturn(List.of(validAsset, staleAsset));
        when(storageService.directoryExists("/photos")).thenReturn(true);
        when(storageService.getFileSize("/photos/a.jpg")).thenReturn(1000L);
        when(storageService.directoryExists("/stale")).thenReturn(false);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).isEmpty();
    }

    @Test
    void getDuplicatedAssets_assetWithZeroFileSize_treatedAsStale() {
        Asset assetA = buildAsset(1L, "a.jpg", "same-hash", "/photos");
        Asset assetB = buildAsset(2L, "b.jpg", "same-hash", "/photos");
        when(assetRepository.findAll()).thenReturn(List.of(assetA, assetB));
        when(storageService.directoryExists("/photos")).thenReturn(true);
        when(storageService.getFileSize("/photos/a.jpg")).thenReturn(1000L);
        when(storageService.getFileSize("/photos/b.jpg")).thenReturn(0L);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).isEmpty();
    }

    @Test
    void getDuplicatedAssets_twoDuplicatePairs_returnsTwoGroups() {
        Asset assetA1 = buildAsset(1L, "a1.jpg", "hash-A", "/photos");
        Asset assetA2 = buildAsset(2L, "a2.jpg", "hash-A", "/photos");
        Asset assetB1 = buildAsset(3L, "b1.jpg", "hash-B", "/photos");
        Asset assetB2 = buildAsset(4L, "b2.jpg", "hash-B", "/photos");
        when(assetRepository.findAll()).thenReturn(List.of(assetA1, assetA2, assetB1, assetB2));
        when(storageService.directoryExists("/photos")).thenReturn(true);
        when(storageService.getFileSize("/photos/a1.jpg")).thenReturn(500L);
        when(storageService.getFileSize("/photos/a2.jpg")).thenReturn(500L);
        when(storageService.getFileSize("/photos/b1.jpg")).thenReturn(800L);
        when(storageService.getFileSize("/photos/b2.jpg")).thenReturn(800L);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).hasSize(2);
    }

    private Asset buildAsset(Long id, String fileName, String hash, String folderPath) {
        Folder folder = new Folder();
        folder.setPath(folderPath);
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFileName(fileName);
        asset.setHash(hash);
        asset.setFolder(folder);
        return asset;
    }
}
