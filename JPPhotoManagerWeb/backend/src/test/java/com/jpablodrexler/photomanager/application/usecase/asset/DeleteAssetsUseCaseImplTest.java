package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAssetsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock AssetExifRepository assetExifRepository;
    @Mock StoragePort storagePort;
    @Mock ThumbnailPort thumbnailPort;
    @InjectMocks DeleteAssetsUseCaseImpl sut;

    @Test
    void execute_permanently_deletesFileAndThumbnailAndRecord() throws IOException {
        Asset asset = buildAsset(1L, "/photos", "img.jpg");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        sut.execute(new Long[]{1L}, true);

        verify(storagePort).deleteFile("/photos/img.jpg");
        verify(thumbnailPort).deleteThumbnail("1.bin");
        verify(assetExifRepository).deleteByAssetId(1L);
        verify(assetRepository).deleteById(1L);
    }

    @Test
    void execute_permanently_fileDeleteThrows_skipsRecordDeletion() throws IOException {
        Asset asset = buildAsset(2L, "/photos", "img.jpg");
        when(assetRepository.findAllById(List.of(2L))).thenReturn(List.of(asset));
        doThrow(new IOException("disk error")).when(storagePort).deleteFile(any());

        sut.execute(new Long[]{2L}, true);

        verify(thumbnailPort, never()).deleteThumbnail(any());
        verify(assetExifRepository, never()).deleteByAssetId(any());
        verify(assetRepository, never()).deleteById(2L);
    }

    @Test
    void execute_softDelete_setsDeletedAtAndSaves() {
        Asset asset = buildAsset(3L, "/photos", "img.jpg");
        when(assetRepository.findAllById(List.of(3L))).thenReturn(List.of(asset));
        when(assetRepository.save(any())).thenReturn(asset);

        sut.execute(new Long[]{3L}, false);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(assetExifRepository, never()).deleteByAssetId(any());
    }

    private static Asset buildAsset(Long id, String folderPath, String fileName) {
        Folder folder = Folder.builder().folderId(1L).path(folderPath).build();
        return Asset.builder().assetId(id).folder(folder).fileName(fileName).build();
    }
}
