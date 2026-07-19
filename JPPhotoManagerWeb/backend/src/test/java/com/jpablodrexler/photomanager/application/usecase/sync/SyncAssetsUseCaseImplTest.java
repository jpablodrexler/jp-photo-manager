package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncAssetsUseCaseImplTest {

    @Mock SyncConfigRepository syncConfigRepository;
    @Mock StoragePort storagePort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks SyncAssetsUseCaseImpl sut;

    @Test
    void execute_noDefinitions_publishesDoneWithEmptyResults() {
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of());

        sut.execute(0L);

        ArgumentCaptor<SyncProgressMessage> captor = ArgumentCaptor.forClass(SyncProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.sync.progress"), eq("0"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().results()).isEmpty();
    }

    @Test
    void execute_sourceDirectoryNotExist_publishesFailureResult() {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(false);

        sut.execute(0L);

        ArgumentCaptor<SyncProgressMessage> captor = ArgumentCaptor.forClass(SyncProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.sync.progress"), eq("0"), captor.capture());
        SyncProgressMessage done = captor.getValue();
        assertThat(done.done()).isTrue();
        assertThat(done.results().get(0).isSuccess()).isFalse();
        assertThat(done.results().get(0).getMessage()).contains("does not exist");
    }

    @Test
    void execute_copiesNewFilesFromSourceToDestination() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/a.jpg"));
        when(storagePort.listFiles("/dest")).thenReturn(List.of());

        sut.execute(0L);

        verify(storagePort).copyFile("/src/a.jpg", "/dest/a.jpg");
        ArgumentCaptor<SyncProgressMessage> captor = ArgumentCaptor.forClass(SyncProgressMessage.class);
        verify(kafkaTemplate, atLeastOnce()).send(eq("job.sync.progress"), eq("0"), captor.capture());
        SyncProgressMessage done = captor.getAllValues().stream()
                .filter(SyncProgressMessage::done).findFirst().orElseThrow();
        assertThat(done.results().get(0).getSyncedCount()).isEqualTo(1);
    }

    @Test
    void execute_skipsExistingFiles() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/a.jpg"));
        when(storagePort.listFiles("/dest")).thenReturn(List.of("/dest/a.jpg"));

        sut.execute(0L);

        verify(storagePort, never()).copyFile(any(), any());
    }

    @Test
    void execute_deleteNotInSource_deletesExtraDestFiles() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, true);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of());
        when(storagePort.listFiles("/dest")).thenReturn(List.of("/dest/extra.jpg"));

        sut.execute(0L);

        verify(storagePort).deleteFile("/dest/extra.jpg");
        ArgumentCaptor<SyncProgressMessage> captor = ArgumentCaptor.forClass(SyncProgressMessage.class);
        verify(kafkaTemplate, atLeastOnce()).send(eq("job.sync.progress"), eq("0"), captor.capture());
        SyncProgressMessage done = captor.getAllValues().stream()
                .filter(SyncProgressMessage::done).findFirst().orElseThrow();
        assertThat(done.results().get(0).getDeletedCount()).isEqualTo(1);
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

        sut.execute(0L);

        verify(storagePort).listFiles("/src/sub");
    }

    @Test
    void execute_syncThrows_publishesFailureResult() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dest", false, false);
        when(syncConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists(anyString())).thenReturn(true);
        when(storagePort.listFiles(anyString())).thenThrow(new RuntimeException("IO error"));

        sut.execute(0L);

        ArgumentCaptor<SyncProgressMessage> captor = ArgumentCaptor.forClass(SyncProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.sync.progress"), eq("0"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().results().get(0).isSuccess()).isFalse();
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
