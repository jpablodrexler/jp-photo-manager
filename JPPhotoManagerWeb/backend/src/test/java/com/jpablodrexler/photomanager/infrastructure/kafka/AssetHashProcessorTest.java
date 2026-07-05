package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetHashProcessorTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks AssetHashProcessor sut;

    @Test
    void onAssetUploaded_computesHashAndUpdatesOnlyHashColumns_whenOtherStagesNotDone() throws Exception {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.existsById(1L)).thenReturn(true);
        when(storagePort.computeHash("/photos/img.jpg")).thenReturn("abc123");
        when(assetRepository.completeIfAllStagesFinished(1L)).thenReturn(false);

        sut.onAssetUploaded(event);

        verify(assetRepository).updateHash(eq(1L), eq("abc123"), any());
        verify(assetRepository, never()).save(any());

        ArgumentCaptor<UploadProgressMessage> captor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.upload.progress"), eq("1"), captor.capture());
        assertThat(captor.getValue().stage()).isEqualTo(UploadStage.HASH);
        assertThat(captor.getValue().done()).isFalse();
    }

    @Test
    void onAssetUploaded_lastStageToFinish_alsoPublishesDoneMessage() throws Exception {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.existsById(1L)).thenReturn(true);
        when(storagePort.computeHash("/photos/img.jpg")).thenReturn("abc123");
        when(assetRepository.completeIfAllStagesFinished(1L)).thenReturn(true);

        sut.onAssetUploaded(event);

        ArgumentCaptor<UploadProgressMessage> captor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate, org.mockito.Mockito.times(2))
                .send(eq("job.upload.progress"), eq("1"), captor.capture());
        assertThat(captor.getAllValues().get(1).done()).isTrue();
        assertThat(captor.getAllValues().get(1).failed()).isFalse();
    }

    @Test
    void onAssetUploaded_assetNotFound_throwsWithoutUpdating() {
        AssetUploadedEvent event = new AssetUploadedEvent(99L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sut.onAssetUploaded(event)).isInstanceOf(RuntimeException.class);

        verify(assetRepository, never()).updateHash(any(Long.class), any(), any());
    }

    @Test
    void onAssetUploaded_hashComputationThrows_propagatesForKafkaRetry() throws Exception {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.existsById(1L)).thenReturn(true);
        when(storagePort.computeHash("/photos/img.jpg")).thenThrow(new java.io.IOException("read error"));

        assertThatThrownBy(() -> sut.onAssetUploaded(event)).isInstanceOf(RuntimeException.class);

        verify(assetRepository, never()).updateHash(any(Long.class), any(), any());
    }
}
