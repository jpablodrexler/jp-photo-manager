package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Optional;

/**
 * Dedicated container factories for the three kafka-async-upload stage processors
 * ({@code asset-hash-processor} / {@code asset-exif-processor} / {@code asset-thumbnail-processor}),
 * each configured with a bounded retry policy (3 retries, 2s apart, per {@code design.md} decision 5).
 * When retries are exhausted for a given {@code AssetUploadedEvent}, the recoverer marks the asset
 * {@code FAILED} and publishes a failed {@code UploadProgressMessage} so the frontend's SSE observer
 * surfaces the failure instead of waiting indefinitely; the asset can later be re-triggered via
 * {@code POST /api/assets/{id}/reprocess}.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UploadProcessorKafkaConfig {

    private static final long RETRY_INTERVAL_MS = 2000L;
    private static final long RETRY_ATTEMPTS = 3L;

    private final ConsumerFactory<Object, Object> consumerFactory;
    private final AssetRepository assetRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> assetHashProcessorContainerFactory() {
        return buildFactory(UploadStage.HASH);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> assetExifProcessorContainerFactory() {
        return buildFactory(UploadStage.EXIF);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> assetThumbnailProcessorContainerFactory() {
        return buildFactory(UploadStage.THUMBNAIL);
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> buildFactory(UploadStage stage) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, exception) -> recoverFailedStage(record, stage, exception),
                new FixedBackOff(RETRY_INTERVAL_MS, RETRY_ATTEMPTS)));
        return factory;
    }

    private void recoverFailedStage(ConsumerRecord<?, ?> record, UploadStage stage, Exception exception) {
        if (!(record.value() instanceof AssetUploadedEvent event)) {
            log.error("Retry exhausted for {} stage on unrecognized record value type {}: {}", stage,
                    record.value() != null ? record.value().getClass() : "null", exception.getMessage());
            return;
        }
        log.error("Retry exhausted for {} stage, assetId={}: {}", stage, event.assetId(), exception.getMessage());

        Optional<Asset> assetOpt = assetRepository.findById(event.assetId());
        if (assetOpt.isPresent()) {
            Asset asset = assetOpt.get();
            asset.setProcessingStatus(ProcessingStatus.FAILED);
            assetRepository.save(asset);
        } else {
            log.warn("Asset {} not found while recovering failed {} stage", event.assetId(), stage);
        }

        kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                UploadProgressMessage.failed(event.assetId(), stage));
    }
}
