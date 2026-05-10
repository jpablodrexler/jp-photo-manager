package com.jpablodrexler.photomanager.infrastructure.service;

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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveAssetsServiceImplTest {

    @Mock
    AssetRepository assetRepository;

    @Mock
    StorageService storageService;

    @Mock
    ThumbnailStorageService thumbnailStorageService;

    @InjectMocks
    MoveAssetsServiceImpl sut;

    @Test
    void moveAssets_preserveOriginalTrue_copiesFileWithoutMoving() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        sut.moveAssets(new Asset[]{asset}, destination, true);

        verify(storageService).copyFile("/source/photo.jpg", "/dest/photo.jpg");
        verify(storageService, never()).moveFile(any(), any());
    }

    @Test
    void moveAssets_preserveOriginalFalse_movesFileWithoutCopying() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        sut.moveAssets(new Asset[]{asset}, destination, false);

        verify(storageService).moveFile("/source/photo.jpg", "/dest/photo.jpg");
        verify(storageService, never()).copyFile(any(), any());
    }

    @Test
    void moveAssets_destinationDirectoryMissing_createsDirectoryBeforeTransfer() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(false);

        sut.moveAssets(new Asset[]{asset}, destination, false);

        verify(storageService).createDirectory("/dest");
    }

    @Test
    void moveAssets_destinationDirectoryExists_doesNotCreateDirectory() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        sut.moveAssets(new Asset[]{asset}, destination, false);

        verify(storageService, never()).createDirectory(any());
    }

    @Test
    void moveAssets_success_updatesAssetFolderAndPersists() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        sut.moveAssets(new Asset[]{asset}, destination, false);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getFolder()).isEqualTo(destination);
    }

    @Test
    void moveAssets_success_returnsTrue() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        boolean result = sut.moveAssets(new Asset[]{asset}, destination, false);

        assertThat(result).isTrue();
    }

    @Test
    void moveAssets_ioExceptionDuringMove_returnsFalse() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);
        doThrow(new IOException("disk error")).when(storageService).moveFile(any(), any());

        boolean result = sut.moveAssets(new Asset[]{asset}, destination, false);

        assertThat(result).isFalse();
    }

    @Test
    void moveAssets_ioExceptionDuringCopy_returnsFalse() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);
        doThrow(new IOException("disk error")).when(storageService).copyFile(any(), any());

        boolean result = sut.moveAssets(new Asset[]{asset}, destination, true);

        assertThat(result).isFalse();
    }

    @Test
    void moveAssets_multipleAssets_processesAll() throws IOException {
        Asset asset1 = buildAsset(1L, "photo1.jpg", "/source");
        Asset asset2 = buildAsset(2L, "photo2.jpg", "/source");
        Folder destination = buildFolder("/dest");
        when(storageService.directoryExists("/dest")).thenReturn(true);

        sut.moveAssets(new Asset[]{asset1, asset2}, destination, true);

        verify(storageService).copyFile("/source/photo1.jpg", "/dest/photo1.jpg");
        verify(storageService).copyFile("/source/photo2.jpg", "/dest/photo2.jpg");
        verify(assetRepository, times(2)).save(any());
    }

    @Test
    void deleteAssets_deleteFileFalse_doesNotCallDeleteFile() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset}, false);

        verify(storageService, never()).deleteFile(any());
    }

    @Test
    void deleteAssets_deleteFileTrue_deletesFileFromDisk() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset}, true);

        verify(storageService).deleteFile("/photos/photo.jpg");
    }

    @Test
    void deleteAssets_deleteFileTrue_deletesThumbnailAndRemovesFromRepository() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset}, true);

        verify(thumbnailStorageService).deleteThumbnail(eq("1.bin"));
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssets_deleteFileFalse_softDeletesSetsDeletedAtAndDoesNotDeleteThumbnail() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset}, false);

        verify(thumbnailStorageService, never()).deleteThumbnail(any());
        verify(assetRepository, never()).delete(any(Asset.class));
        assertThat(asset.getDeletedAt()).isNotNull();
        verify(assetRepository).save(asset);
    }

    @Test
    void deleteAssets_deleteFileTrue_callsDeleteFileAndThumbnailAndDeletesRow() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset}, true);

        verify(storageService).deleteFile("/photos/photo.jpg");
        verify(thumbnailStorageService).deleteThumbnail("1.bin");
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssets_ioExceptionOnFileDeletion_stillDeletesThumbnailAndRepository() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg", "/photos");
        doThrow(new IOException("disk error")).when(storageService).deleteFile(any());

        sut.deleteAssets(new Asset[]{asset}, true);

        verify(thumbnailStorageService).deleteThumbnail("1.bin");
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssets_multipleAssets_deletesAll() throws IOException {
        Asset asset1 = buildAsset(1L, "photo1.jpg", "/photos");
        Asset asset2 = buildAsset(2L, "photo2.jpg", "/photos");

        sut.deleteAssets(new Asset[]{asset1, asset2}, true);

        verify(storageService).deleteFile("/photos/photo1.jpg");
        verify(storageService).deleteFile("/photos/photo2.jpg");
        verify(assetRepository).delete(asset1);
        verify(assetRepository).delete(asset2);
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

    private Folder buildFolder(String path) {
        Folder folder = new Folder();
        folder.setPath(path);
        return folder;
    }
}
