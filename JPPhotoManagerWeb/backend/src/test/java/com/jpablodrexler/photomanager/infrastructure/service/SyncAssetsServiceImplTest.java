package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.repository.SyncAssetsConfigRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncAssetsServiceImplTest {

    @Mock
    SyncAssetsConfigRepository configRepository;

    @Mock
    StorageService storageService;

    @InjectMocks
    SyncAssetsServiceImpl sut;

    @Test
    void executeAsync_noDefinitions_returnsEmptyList() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results).isEmpty();
    }

    @Test
    void executeAsync_sourceDirectoryMissing_resultMarkedFailed() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists("/source")).thenReturn(false);

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).getMessage()).contains("/source");
    }

    @Test
    void executeAsync_destinationDirectoryMissing_createsDestinationDirectory() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists("/source")).thenReturn(true);
        when(storageService.directoryExists("/dest")).thenReturn(false);
        when(storageService.listFiles(any())).thenReturn(List.of());

        sut.executeAsync(null).get();

        verify(storageService).createDirectory("/dest");
    }

    @Test
    void executeAsync_destinationDirectoryExists_doesNotCreateDirectory() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles(any())).thenReturn(List.of());

        sut.executeAsync(null).get();

        verify(storageService, never()).createDirectory(any());
    }

    @Test
    void executeAsync_newFileInSource_copiesFileToDestination() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.jpg"));
        when(storageService.listFiles("/dest")).thenReturn(List.of());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        verify(storageService).copyFile("/source/photo.jpg", "/dest/photo.jpg");
        assertThat(results.get(0).getSyncedCount()).isEqualTo(1);
    }

    @Test
    void executeAsync_fileAlreadyInDestination_doesNotCopyAgain() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.jpg"));
        when(storageService.listFiles("/dest")).thenReturn(List.of("/dest/photo.jpg"));

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        verify(storageService, never()).copyFile(any(), any());
        assertThat(results.get(0).getSyncedCount()).isZero();
    }

    @Test
    void executeAsync_deleteNotInSourceTrue_deletesOrphanedDestFile() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, true)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of());
        when(storageService.listFiles("/dest")).thenReturn(List.of("/dest/orphan.jpg"));

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        verify(storageService).deleteFile("/dest/orphan.jpg");
        assertThat(results.get(0).getDeletedCount()).isEqualTo(1);
    }

    @Test
    void executeAsync_deleteNotInSourceFalse_keepsOrphanedDestFile() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of());
        when(storageService.listFiles("/dest")).thenReturn(List.of("/dest/orphan.jpg"));

        sut.executeAsync(null).get();

        verify(storageService, never()).deleteFile(any());
    }

    @Test
    void executeAsync_callbackProvided_invokesCallbackOnEachCopiedFile() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.jpg"));
        when(storageService.listFiles("/dest")).thenReturn(List.of());
        List<String> messages = new ArrayList<>();

        sut.executeAsync(messages::add).get();

        assertThat(messages).anyMatch(m -> m.contains("photo.jpg"));
    }

    @Test
    void executeAsync_includeSubFoldersTrue_recursesIntoSubDirectories() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", true, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of());
        when(storageService.listFiles("/dest")).thenReturn(List.of());
        when(storageService.listSubDirectories("/source")).thenReturn(List.of("/source/sub"));
        when(storageService.listFiles("/source/sub")).thenReturn(List.of("/source/sub/photo.jpg"));
        when(storageService.listFiles("/dest/sub")).thenReturn(List.of());
        when(storageService.listSubDirectories("/source/sub")).thenReturn(List.of());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        verify(storageService).copyFile("/source/sub/photo.jpg", "/dest/sub/photo.jpg");
        assertThat(results.get(0).getSyncedCount()).isEqualTo(1);
    }

    @Test
    void executeAsync_includeSubFoldersFalse_doesNotProcessSubDirectories() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles(any())).thenReturn(List.of());

        sut.executeAsync(null).get();

        verify(storageService, never()).listSubDirectories(any());
    }

    @Test
    void executeAsync_ioExceptionDuringCopy_resultMarkedFailed() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.jpg"));
        when(storageService.listFiles("/dest")).thenReturn(List.of());
        doThrow(new IOException("disk error")).when(storageService).copyFile(any(), any());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).getMessage()).contains("disk error");
    }

    @Test
    void executeAsync_successfulSync_resultMarkedSuccess() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles(any())).thenReturn(List.of());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).isSuccess()).isTrue();
    }

    @Test
    void executeAsync_multipleDefinitions_returnsOneResultPerDefinition() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(
                buildDef("/src1", "/dst1", false, false),
                buildDef("/src2", "/dst2", false, false)));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles(any())).thenReturn(List.of());

        List<SyncAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results).hasSize(2);
    }

    private SyncAssetsDirectoriesDefinition buildDef(String source, String dest,
                                                      boolean includeSubFolders, boolean deleteNotInSource) {
        SyncAssetsDirectoriesDefinition def = new SyncAssetsDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        def.setIncludeSubFolders(includeSubFolders);
        def.setDeleteAssetsNotInSource(deleteNotInSource);
        return def;
    }
}
