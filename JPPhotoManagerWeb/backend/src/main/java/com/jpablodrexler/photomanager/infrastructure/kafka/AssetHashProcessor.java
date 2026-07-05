package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Persistent-consumer-group processor (kafka-async-upload, stage 1 of 3): computes the SHA-256 hash
 * for a newly-uploaded asset. Runs independently of {@link AssetExifProcessor} and
 * {@link AssetThumbnailProcessor} — a failure here does not block or roll back the other two stages
 * (see {@code AuditLogKafkaListener} for the same explicit-persistent-groupId pattern).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetHashProcessor {

    private static final String CONSUMER_GROUP = "asset-hash-processor";

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "asset.uploaded", groupId = CONSUMER_GROUP,
            containerFactory = "assetHashProcessorContainerFactory")
    @Transactional
    public void onAssetUploaded(AssetUploadedEvent event) {
        try {
            if (!assetRepository.existsById(event.assetId())) {
                throw new NoSuchElementException("Asset not found: " + event.assetId());
            }
            String hash = storagePort.computeHash(event.filePath());

            // Targeted column update, not findById()+save(): the hash/exif/thumbnail processors run
            // concurrently against the same row, and a full-entity save() from a stale in-memory
            // snapshot would silently overwrite whichever other stage had already committed.
            assetRepository.updateHash(event.assetId(), hash, LocalDateTime.now());

            kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                    UploadProgressMessage.stageComplete(event.assetId(), UploadStage.HASH));

            if (assetRepository.completeIfAllStagesFinished(event.assetId())) {
                kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                        UploadProgressMessage.done(event.assetId()));
            }
        } catch (Exception e) {
            log.error("Hash processing failed for assetId={}", event.assetId(), e);
            throw new RuntimeException("Hash processing failed for assetId=" + event.assetId(), e);
        }
    }
}
