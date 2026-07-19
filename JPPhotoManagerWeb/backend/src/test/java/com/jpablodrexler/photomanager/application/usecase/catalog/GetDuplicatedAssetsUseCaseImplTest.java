package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDuplicatedAssetsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @InjectMocks GetDuplicatedAssetsUseCaseImpl sut;

    @Test
    void execute_noDuplicates_returnsEmptyList() {
        Asset a1 = buildAsset(1L, "hash-aaa");
        Asset a2 = buildAsset(2L, "hash-bbb");
        when(assetRepository.findNotDeleted()).thenReturn(List.of(a1, a2));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.getFileSize(anyString())).thenReturn(100L);

        List<List<Asset>> result = sut.execute();

        assertThat(result).isEmpty();
    }

    @Test
    void execute_withDuplicates_returnsGroups() {
        Asset a1 = buildAsset(1L, "hash-dup");
        Asset a2 = buildAsset(2L, "hash-dup");
        Asset a3 = buildAsset(3L, "hash-unique");
        when(assetRepository.findNotDeleted()).thenReturn(List.of(a1, a2, a3));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.getFileSize(anyString())).thenReturn(100L);

        List<List<Asset>> result = sut.execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactlyInAnyOrder(a1, a2);
    }

    @Test
    void execute_invalidAssets_excludedFromDuplicateDetection() {
        Asset valid = buildAsset(1L, "hash-dup");
        Asset noDir = buildAsset(2L, "hash-dup");
        Asset zeroSize = buildAsset(3L, "hash-dup");
        when(assetRepository.findNotDeleted()).thenReturn(List.of(valid, noDir, zeroSize));
        when(storagePort.directoryExists("/photos")).thenReturn(true);
        when(storagePort.directoryExists("/nodir")).thenReturn(false);
        when(storagePort.getFileSize("/photos/photo1.jpg")).thenReturn(100L);
        when(storagePort.getFileSize("/photos/photo3.jpg")).thenReturn(0L);

        List<List<Asset>> result = sut.execute();

        assertThat(result).isEmpty();
    }

    private static Asset buildAsset(Long id, String hash) {
        String dirPath = id == 2L ? "/nodir" : "/photos";
        Folder folder = Folder.builder().folderId(id).path(dirPath).build();
        return Asset.builder().assetId(id).folder(folder).fileName("photo" + id + ".jpg").hash(hash).build();
    }
}
