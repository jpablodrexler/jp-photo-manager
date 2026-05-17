package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.RecentTargetPathRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveAssetsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;
    @Mock RecentTargetPathRepository recentTargetPathRepository;
    @InjectMocks MoveAssetsUseCaseImpl sut;

    private static final String ROOT = "/tmp/photos";
    private static final String DEST = "/tmp/photos/dest";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", ROOT);
    }

    @Test
    void execute_destinationOutsideRoot_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.execute(new Long[]{1L}, "/etc/passwd", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the allowed catalog roots");
    }

    @Test
    void execute_preserveOriginal_copiesFileAndUpdatesAssetFolder() throws IOException {
        Asset asset = buildAsset(1L, "/tmp/photos/src", "img.jpg");
        Folder destFolder = Folder.builder().folderId(2L).path(DEST).build();
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath(DEST)).thenReturn(Optional.of(destFolder));
        when(storagePort.directoryExists(DEST)).thenReturn(true);
        when(assetRepository.save(any())).thenReturn(asset);
        when(recentTargetPathRepository.existsByPath(DEST)).thenReturn(true);

        boolean result = sut.execute(new Long[]{1L}, DEST, true);

        assertThat(result).isTrue();
        verify(storagePort).copyFile("/tmp/photos/src/img.jpg", DEST + "/img.jpg");
        verify(assetRepository).save(asset);
        assertThat(asset.getFolder()).isEqualTo(destFolder);
    }

    @Test
    void execute_moveFile_createsDirectoryWhenMissing() throws IOException {
        Asset asset = buildAsset(1L, "/tmp/photos/src", "img.jpg");
        Folder destFolder = Folder.builder().folderId(2L).path(DEST).build();
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath(DEST)).thenReturn(Optional.of(destFolder));
        when(storagePort.directoryExists(DEST)).thenReturn(false);
        when(assetRepository.save(any())).thenReturn(asset);
        when(recentTargetPathRepository.existsByPath(DEST)).thenReturn(true);

        boolean result = sut.execute(new Long[]{1L}, DEST, false);

        assertThat(result).isTrue();
        verify(storagePort).createDirectory(DEST);
        verify(storagePort).moveFile("/tmp/photos/src/img.jpg", DEST + "/img.jpg");
    }

    @Test
    void execute_copyFails_returnsFalse() throws IOException {
        Asset asset = buildAsset(1L, "/tmp/photos/src", "img.jpg");
        Folder destFolder = Folder.builder().folderId(2L).path(DEST).build();
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath(DEST)).thenReturn(Optional.of(destFolder));
        when(storagePort.directoryExists(DEST)).thenReturn(true);
        doThrow(new IOException("disk full")).when(storagePort).copyFile(any(), any());

        boolean result = sut.execute(new Long[]{1L}, DEST, true);

        assertThat(result).isFalse();
    }

    @Test
    void execute_savesNewRecentPath() throws IOException {
        Asset asset = buildAsset(1L, "/tmp/photos/src", "img.jpg");
        Folder destFolder = Folder.builder().folderId(2L).path(DEST).build();
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath(DEST)).thenReturn(Optional.of(destFolder));
        when(storagePort.directoryExists(DEST)).thenReturn(true);
        when(assetRepository.save(any())).thenReturn(asset);
        when(recentTargetPathRepository.existsByPath(DEST)).thenReturn(false);
        when(recentTargetPathRepository.findAllOrderByIdDesc()).thenReturn(List.of());

        sut.execute(new Long[]{1L}, DEST, true);

        verify(recentTargetPathRepository).save(any(RecentTargetPath.class));
    }

    @Test
    void execute_evictsOldPathsWhenOverLimit() throws IOException {
        Asset asset = buildAsset(1L, "/tmp/photos/src", "img.jpg");
        Folder destFolder = Folder.builder().folderId(2L).path(DEST).build();
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath(DEST)).thenReturn(Optional.of(destFolder));
        when(storagePort.directoryExists(DEST)).thenReturn(true);
        when(assetRepository.save(any())).thenReturn(asset);
        when(recentTargetPathRepository.existsByPath(DEST)).thenReturn(false);

        List<RecentTargetPath> many = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            many.add(RecentTargetPath.builder().id((long) i).path("/tmp/photos/p" + i).build());
        }
        when(recentTargetPathRepository.findAllOrderByIdDesc()).thenReturn(many);

        sut.execute(new Long[]{1L}, DEST, true);

        verify(recentTargetPathRepository).deleteAll(many.subList(20, 21));
    }

    private static Asset buildAsset(Long id, String folderPath, String fileName) {
        Folder folder = Folder.builder().folderId(id).path(folderPath).build();
        return Asset.builder().assetId(id).folder(folder).fileName(fileName).build();
    }
}
