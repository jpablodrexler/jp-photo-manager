package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.RenameAssetsResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
