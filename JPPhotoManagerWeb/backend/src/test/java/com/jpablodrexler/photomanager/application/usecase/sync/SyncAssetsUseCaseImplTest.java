package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncAssetsUseCaseImplTest {

    @Mock SyncConfigRepository syncConfigRepository;
    @Mock StoragePort storagePort;
    @InjectMocks SyncAssetsUseCaseImpl sut;

    @Test
    void execute_noDefinitions_returnsEmptyList() throws Exception {
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of());

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result).isEmpty();
    }

    @Test
    void execute_sourceDirectoryNotExist_returnsFailureResult() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(false);

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isSuccess()).isFalse();
        assertThat(result.get(0).getMessage()).contains("does not exist");
    }

    @Test
    void execute_copiesNewFilesFromSourceToDestination() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/a.jpg"));
        when(storagePort.listFiles("/dest")).thenReturn(List.of());

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result.get(0).getSyncedCount()).isEqualTo(1);
        verify(storagePort).copyFile("/src/a.jpg", "/dest/a.jpg");
    }

    @Test
    void execute_skipsExistingFiles() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/a.jpg"));
        when(storagePort.listFiles("/dest")).thenReturn(List.of("/dest/a.jpg"));

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result.get(0).getSyncedCount()).isZero();
        verify(storagePort, never()).copyFile(any(), any());
    }

    @Test
    void execute_deleteNotInSource_deletesExtraDestFiles() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, true);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of());
        when(storagePort.listFiles("/dest")).thenReturn(List.of("/dest/extra.jpg"));

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result.get(0).getDeletedCount()).isEqualTo(1);
        verify(storagePort).deleteFile("/dest/extra.jpg");
    }

    @Test
    void execute_includeSubFolders_recursiveSync() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", true, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of());
        when(storagePort.listFiles("/dest")).thenReturn(List.of());
        when(storagePort.listSubDirectories("/src")).thenReturn(List.of("/src/sub"));
        when(storagePort.listFiles("/src/sub")).thenReturn(List.of());
        when(storagePort.listFiles("/dest/sub")).thenReturn(List.of());
        when(storagePort.listSubDirectories("/src/sub")).thenReturn(List.of());

        sut.execute(null).get();

        verify(storagePort).listFiles("/src/sub");
    }

    @Test
    void execute_syncThrows_returnsFailureResult() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles(anyString())).thenThrow(new RuntimeException("IO error"));

        List<SyncAssetsResult> result = sut.execute(null).get();

        assertThat(result.get(0).isSuccess()).isFalse();
    }

    private static SyncDirectoriesDefinition buildDef(String src, String dest, boolean subs, boolean delete) {
        return SyncDirectoriesDefinition.builder()
                .sourceDirectory(src)
                .destinationDirectory(dest)
                .includeSubFolders(subs)
                .deleteAssetsNotInSource(delete)
                .build();
    }
}
