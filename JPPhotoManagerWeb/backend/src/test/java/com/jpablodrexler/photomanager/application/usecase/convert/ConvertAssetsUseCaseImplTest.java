package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.application.dto.ConvertProgressMessage;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertAssetsUseCaseImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock ConvertConfigRepository convertConfigRepository;
    @Mock StoragePort storagePort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks ConvertAssetsUseCaseImpl sut;

    @Test
    void execute_noDefinitions_publishesDoneWithEmptyResults() {
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of());

        sut.execute(0L, USER_ID);

        ArgumentCaptor<ConvertProgressMessage> captor = ArgumentCaptor.forClass(ConvertProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.convert.progress"), eq("0"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().results()).isEmpty();
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
    }

    @Test
    void execute_sourceDirectoryNotExist_publishesFailureResult() {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(false);

        sut.execute(0L, USER_ID);

        ArgumentCaptor<ConvertProgressMessage> captor = ArgumentCaptor.forClass(ConvertProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.convert.progress"), eq("0"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().results().get(0).isSuccess()).isFalse();
    }

    @Test
    void execute_convertsPngFilesInSourceDirectory() throws Exception {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.png"));

        sut.execute(0L, USER_ID);

        verify(storagePort).convertPngToJpeg("/src/photo.png", "/dest/photo.jpg");
    }

    @Test
    void execute_skipsNonPngFiles() throws Exception {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.jpg", "/src/doc.txt"));

        sut.execute(0L, USER_ID);

        verify(storagePort, never()).convertPngToJpeg(any(), any());
    }

    @Test
    void execute_publishesDoneWithConvertedCount() throws Exception {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.png"));

        sut.execute(0L, USER_ID);

        ArgumentCaptor<ConvertProgressMessage> captor = ArgumentCaptor.forClass(ConvertProgressMessage.class);
        verify(kafkaTemplate, atLeastOnce()).send(eq("job.convert.progress"), eq("0"), captor.capture());
        ConvertProgressMessage done = captor.getAllValues().stream()
                .filter(ConvertProgressMessage::done).findFirst().orElseThrow();
        assertThat(done.results().get(0).getConvertedCount()).isEqualTo(1);
    }

    @Test
    void execute_convertFails_recordsFailedCount() throws Exception {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
        when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
        when(storagePort.directoryExists("/src")).thenReturn(true);
        when(storagePort.directoryExists("/dest")).thenReturn(true);
        when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.png"));
        doThrow(new IOException("disk full")).when(storagePort).convertPngToJpeg(any(), any());

        sut.execute(0L, USER_ID);

        ArgumentCaptor<ConvertProgressMessage> captor = ArgumentCaptor.forClass(ConvertProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.convert.progress"), eq("0"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().results().get(0).getFailedCount()).isEqualTo(1);
    }

    private static ConvertDirectoriesDefinition buildDef(String src, String dest) {
        return ConvertDirectoriesDefinition.builder()
                .sourceDirectory(src)
                .destinationDirectory(dest)
                .build();
    }
}
