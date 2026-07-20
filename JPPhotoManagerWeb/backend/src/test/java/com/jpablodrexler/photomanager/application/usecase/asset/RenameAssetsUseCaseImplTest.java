package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.RenameAssetsResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenameAssetsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks RenameAssetsUseCaseImpl sut;

    private static final String FOLDER_PATH = "/photos";

    // --- token resolution ---

    @Test
    void execute_dateToken_resolvesToFormattedDate() {
        Asset asset = buildAsset(1L, "IMG_001.jpg", LocalDateTime.of(2024, 6, 15, 10, 30));
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "{date:yyyy-MM-dd}.{ext}", false);

        assertThat(result.previews()).hasSize(1);
        assertThat(result.previews().get(0).newName()).isEqualTo("2024-06-15.jpg");
    }

    @Test
    void execute_indexToken_resolvesToZeroPaddedPosition() {
        Asset a1 = buildAsset(1L, "a.jpg", null);
        Asset a2 = buildAsset(2L, "b.jpg", null);
        Asset a3 = buildAsset(3L, "c.jpg", null);
        when(assetRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(a1, a2, a3));

        RenameAssetsResult result = sut.execute(new Long[]{1L, 2L, 3L}, "photo_{index:03d}.{ext}", false);

        assertThat(result.previews().get(0).newName()).isEqualTo("photo_001.jpg");
        assertThat(result.previews().get(1).newName()).isEqualTo("photo_002.jpg");
        assertThat(result.previews().get(2).newName()).isEqualTo("photo_003.jpg");
    }

    @Test
    void execute_originalToken_resolvesToBaseFileNameWithoutExtension() {
        Asset asset = buildAsset(1L, "IMG_4587.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "{original}_edited.{ext}", false);

        assertThat(result.previews().get(0).newName()).isEqualTo("IMG_4587_edited.jpg");
    }

    @Test
    void execute_extToken_resolvesToLowercaseExtension() {
        Asset asset = buildAsset(1L, "DSC001.JPG", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "scan_{index:01d}.{ext}", false);

        assertThat(result.previews().get(0).newName()).isEqualTo("scan_1.jpg");
    }

    @Test
    void execute_patternWithoutExtToken_appendsOriginalExtension() {
        Asset asset = buildAsset(1L, "photo.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "vacation", false);

        assertThat(result.previews().get(0).newName()).isEqualTo("vacation.jpg");
    }

    // --- date format validation ---

    @Test
    void execute_invalidDateFormatWithSpecialChars_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "{date:INVALID!!!}", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_DATE_FORMAT");
    }

    @Test
    void execute_dateFormatWithUnknownPatternLetter_throwsIllegalArgumentException() {
        // 'J' is not a valid DateTimeFormatter pattern letter in Java 21
        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "{date:J}", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_DATE_FORMAT");
    }

    // --- collision detection ---

    @Test
    void execute_withinBatchCollision_throwsIllegalArgumentException() {
        Asset a1 = buildAsset(1L, "a.jpg", LocalDateTime.of(2024, 1, 1, 0, 0));
        Asset a2 = buildAsset(2L, "b.jpg", LocalDateTime.of(2024, 1, 1, 0, 0));
        when(assetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a1, a2));

        // Both assets have the same date, so {date:yyyy-MM-dd}.{ext} produces the same name
        assertThatThrownBy(() -> sut.execute(new Long[]{1L, 2L}, "{date:yyyy-MM-dd}.{ext}", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ASSET_NAME_COLLISION");
    }

    @Test
    void execute_existingAssetCollisionInApplyMode_throwsIllegalArgumentException() throws Exception {
        Asset batchAsset = buildAsset(1L, "old.jpg", null);
        Asset folderAsset = buildAsset(99L, "vacation.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(batchAsset));
        when(assetRepository.findByFolder(batchAsset.getFolder())).thenReturn(List.of(batchAsset, folderAsset));

        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "vacation", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ASSET_NAME_COLLISION");

        verify(storagePort, never()).moveFile(any(), any());
    }

    @Test
    void execute_uncatalogedFileAlreadyOnDiskAtTargetName_throwsIllegalArgumentExceptionAndDoesNotOverwriteIt() throws Exception {
        Asset batchAsset = buildAsset(1L, "old.png", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(batchAsset));
        when(assetRepository.findByFolder(batchAsset.getFolder())).thenReturn(List.of(batchAsset));
        // logo.png exists on disk but was never catalogued, so the DB-only check alone would miss it
        when(storagePort.listFiles(FOLDER_PATH)).thenReturn(List.of(FOLDER_PATH + "/logo.png"));

        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "logo.{ext}", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ASSET_NAME_COLLISION");

        verify(storagePort, never()).moveFile(any(), any());
    }

    @Test
    void execute_renameOntoOwnCurrentFileName_doesNotFlagFalseCollisionFromDisk() throws Exception {
        // A no-op rename (pattern happens to reproduce the current file name) must not be
        // treated as a collision against the asset's own file already present on disk.
        Asset asset = buildAsset(1L, "old.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(assetRepository.findByFolder(asset.getFolder())).thenReturn(List.of(asset));
        when(storagePort.listFiles(FOLDER_PATH)).thenReturn(List.of(FOLDER_PATH + "/old.jpg"));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "old.{ext}", true);

        assertThat(result.applied()).isTrue();
        verify(storagePort).moveFile(FOLDER_PATH + "/old.jpg", FOLDER_PATH + "/old.jpg");
    }

    @Test
    void execute_multipleAssetsInSameFolder_checksCollisionsForEachAssetWithoutError() throws Exception {
        // Regression test: checkFolderCollisions used to remove a folder's entry from its
        // lookup map after checking the first asset, so a second batch member in the same
        // folder would hit a NullPointerException instead of being checked at all.
        Asset a1 = buildAsset(1L, "a.jpg", null);
        Asset a2 = buildAsset(2L, "b.jpg", null);
        Asset unrelated = buildAsset(99L, "keepme.jpg", null);
        when(assetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a1, a2));
        when(assetRepository.findByFolder(a1.getFolder())).thenReturn(List.of(a1, a2, unrelated));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RenameAssetsResult result = sut.execute(new Long[]{1L, 2L}, "new_{index:01d}.{ext}", true);

        assertThat(result.applied()).isTrue();
        verify(storagePort).moveFile(FOLDER_PATH + "/a.jpg", FOLDER_PATH + "/new_1.jpg");
        verify(storagePort).moveFile(FOLDER_PATH + "/b.jpg", FOLDER_PATH + "/new_2.jpg");
    }

    // --- preview-only mode ---

    @Test
    void execute_previewOnly_doesNotMutateFilesOrDb() throws Exception {
        Asset asset = buildAsset(1L, "photo.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "renamed.{ext}", false);

        assertThat(result.applied()).isFalse();
        assertThat(result.previews().get(0).newName()).isEqualTo("renamed.jpg");
        verify(storagePort, never()).moveFile(any(), any());
        verify(assetRepository, never()).save(any());
    }

    // --- apply mode ---

    @Test
    void execute_applied_movesFileAndUpdatesDbFileName() throws Exception {
        Asset asset = buildAsset(1L, "old.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(assetRepository.findByFolder(asset.getFolder())).thenReturn(List.of(asset));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RenameAssetsResult result = sut.execute(new Long[]{1L}, "new.{ext}", true);

        assertThat(result.applied()).isTrue();
        verify(storagePort).moveFile(FOLDER_PATH + "/old.jpg", FOLDER_PATH + "/new.jpg");
        verify(assetRepository).save(argThat(a -> "new.jpg".equals(a.getFileName())));
    }

    @Test
    void execute_applied_ioExceptionOnMoveFile_throwsRuntimeException() throws Exception {
        Asset asset = buildAsset(1L, "old.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(assetRepository.findByFolder(asset.getFolder())).thenReturn(List.of(asset));
        doThrow(new IOException("disk error")).when(storagePort).moveFile(any(), any());

        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "new.{ext}", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to rename asset");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void execute_applied_dbSaveFailsAfterMoveFile_revertsFileOnDiskAndThrows() throws Exception {
        Asset asset = buildAsset(1L, "old.jpg", null);
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(assetRepository.findByFolder(asset.getFolder())).thenReturn(List.of(asset));
        when(assetRepository.save(any())).thenThrow(new RuntimeException("db unavailable"));

        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "new.{ext}", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");

        verify(storagePort).moveFile(FOLDER_PATH + "/old.jpg", FOLDER_PATH + "/new.jpg");
        verify(storagePort).moveFile(FOLDER_PATH + "/new.jpg", FOLDER_PATH + "/old.jpg");
    }

    @Test
    void execute_applied_secondAssetFailsToMove_firstAssetRenameIsNotRolledBack() throws Exception {
        Folder folder1 = Folder.builder().folderId(10L).path(FOLDER_PATH).build();
        Folder folder2 = Folder.builder().folderId(11L).path("/photos2").build();
        Asset a1 = Asset.builder().assetId(1L).fileName("a.jpg").folder(folder1).build();
        Asset a2 = Asset.builder().assetId(2L).fileName("b.jpg").folder(folder2).build();
        when(assetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a1, a2));
        when(assetRepository.findByFolder(folder1)).thenReturn(List.of(a1));
        when(assetRepository.findByFolder(folder2)).thenReturn(List.of(a2));
        when(assetRepository.save(a1)).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(invocation -> {
            String dest = invocation.getArgument(1);
            if ("/photos2/new_2.jpg".equals(dest)) {
                throw new IOException("disk error");
            }
            return null;
        }).when(storagePort).moveFile(any(), any());

        assertThatThrownBy(() -> sut.execute(new Long[]{1L, 2L}, "new_{index:01d}.{ext}", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to rename asset");

        verify(assetRepository).save(a1);
        assertThat(a1.getFileName()).isEqualTo("new_1.jpg");
        verify(storagePort).moveFile(FOLDER_PATH + "/a.jpg", FOLDER_PATH + "/new_1.jpg");
    }

    // --- helpers ---

    private Asset buildAsset(Long id, String fileName, LocalDateTime fileCreationDateTime) {
        Folder folder = Folder.builder().folderId(10L).path(FOLDER_PATH).build();
        return Asset.builder()
                .assetId(id)
                .fileName(fileName)
                .folder(folder)
                .fileCreationDateTime(fileCreationDateTime)
                .build();
    }
}
