package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Persistent-consumer-group processor (kafka-async-upload, stage 3 of 3): generates the 200x150
 * thumbnail for a newly-uploaded asset and persists it via {@link ThumbnailPort}. Runs independently
 * of {@link AssetHashProcessor} and {@link AssetExifProcessor}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetThumbnailProcessor {

    private static final String CONSUMER_GROUP = "asset-thumbnail-processor";
    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 150;

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "asset.uploaded", groupId = CONSUMER_GROUP,
            containerFactory = "assetThumbnailProcessorContainerFactory")
    @Transactional
    public void onAssetUploaded(AssetUploadedEvent event) {
        try {
            // Read-only lookup, never written back: only used to derive the thumbnail blob name via
            // the canonical Asset.getThumbnailBlobName(), never persisted (see AssetHashProcessor for
            // why a full-entity save() here would race with the other two concurrently-running stages).
            Asset asset = assetRepository.findById(event.assetId())
                    .orElseThrow(() -> new NoSuchElementException("Asset not found: " + event.assetId()));

            byte[] thumbnail = storagePort.generateThumbnail(event.filePath(), THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
            thumbnailPort.saveThumbnail(asset.getThumbnailBlobName(), thumbnail);

            LocalDateTime now = LocalDateTime.now();
            assetRepository.updateThumbnail(event.assetId(), now, now);

            kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                    UploadProgressMessage.stageComplete(event.assetId(), UploadStage.THUMBNAIL));

            if (assetRepository.completeIfAllStagesFinished(event.assetId())) {
                kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                        UploadProgressMessage.done(event.assetId()));
            }
        } catch (Exception e) {
            log.error("Thumbnail processing failed for assetId={}", event.assetId(), e);
            throw new RuntimeException("Thumbnail processing failed for assetId=" + event.assetId(), e);
        }
    }
}
