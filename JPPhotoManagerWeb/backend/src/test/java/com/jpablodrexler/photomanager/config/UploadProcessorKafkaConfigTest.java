package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the kafka-async-upload retry-exhaustion recovery behavior (design.md decision 5, tasks
 * 4.6 / 8.4): after {@code DefaultErrorHandler} exhausts its retries for a given stage, the asset is
 * marked FAILED and a failed {@code UploadProgressMessage} is published.
 */
@ExtendWith(MockitoExtension.class)
class UploadProcessorKafkaConfigTest {

    @Mock ConsumerFactory<Object, Object> consumerFactory;
    @Mock AssetRepository assetRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks UploadProcessorKafkaConfig sut;

    @Test
    void recoverFailedStage_retriesExhausted_marksAssetFailedAndPublishesFailedMessage() {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("asset.uploaded", 0, 0L, "1", event);
        Asset asset = new Asset();
        asset.setAssetId(1L);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        ReflectionTestUtils.invokeMethod(sut, "recoverFailedStage", record, UploadStage.EXIF,
                new RuntimeException("boom"));

        assertThat(asset.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        verify(assetRepository).save(asset);

        ArgumentCaptor<UploadProgressMessage> captor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.upload.progress"), eq("1"), captor.capture());
        assertThat(captor.getValue().done()).isTrue();
        assertThat(captor.getValue().failed()).isTrue();
        assertThat(captor.getValue().stage()).isEqualTo(UploadStage.EXIF);
    }

    @Test
    void recoverFailedStage_assetNotFound_stillPublishesFailedMessage() {
        AssetUploadedEvent event = new AssetUploadedEvent(99L, "/photos/img.jpg", "/photos", "img.jpg");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("asset.uploaded", 0, 0L, "99", event);
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(sut, "recoverFailedStage", record, UploadStage.HASH,
                new RuntimeException("boom"));

        ArgumentCaptor<UploadProgressMessage> captor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.upload.progress"), eq("99"), captor.capture());
        assertThat(captor.getValue().failed()).isTrue();
    }
}
