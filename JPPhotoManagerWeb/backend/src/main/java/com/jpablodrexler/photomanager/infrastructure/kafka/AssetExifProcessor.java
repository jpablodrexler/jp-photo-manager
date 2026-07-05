package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
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
 * Persistent-consumer-group processor (kafka-async-upload, stage 2 of 3): extracts EXIF metadata for
 * a newly-uploaded asset and upserts it into the MongoDB-backed {@link AssetExifRepository}. Runs
 * independently of {@link AssetHashProcessor} and {@link AssetThumbnailProcessor}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetExifProcessor {

    private static final String CONSUMER_GROUP = "asset-exif-processor";

    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final StoragePort storagePort;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "asset.uploaded", groupId = CONSUMER_GROUP,
            containerFactory = "assetExifProcessorContainerFactory")
    @Transactional
    public void onAssetUploaded(AssetUploadedEvent event) {
        try {
            if (!assetRepository.existsById(event.assetId())) {
                throw new NoSuchElementException("Asset not found: " + event.assetId());
            }

            ExifMetadata exif = storagePort.getExifMetadata(event.filePath());
            AssetExif assetExif = assetExifRepository.findByAssetId(event.assetId())
                    .orElseGet(() -> {
                        AssetExif e = new AssetExif();
                        e.setAssetId(event.assetId());
                        return e;
                    });
            assetExif.setCameraMake(exif.cameraMake());
            assetExif.setCameraModel(exif.cameraModel());
            assetExif.setLensModel(exif.lensModel());
            assetExif.setExposureTime(exif.exposureTime());
            assetExif.setFNumber(exif.fNumber());
            assetExif.setIsoSpeed(exif.isoSpeed());
            assetExif.setFocalLength(exif.focalLength());
            assetExif.setDateTaken(exif.dateTaken());
            assetExif.setWidthPixels(exif.widthPixels());
            assetExif.setHeightPixels(exif.heightPixels());
            assetExif.setGpsLatitude(exif.gpsLatitude());
            assetExif.setGpsLongitude(exif.gpsLongitude());
            assetExif.setRawExif(exif.rawExif());
            assetExifRepository.save(assetExif);

            // Targeted column update, not findById()+save(): see AssetHashProcessor for why a
            // full-entity save() would race with the other two concurrently-running stages.
            assetRepository.updateExifCompletedAt(event.assetId(), LocalDateTime.now());

            kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                    UploadProgressMessage.stageComplete(event.assetId(), UploadStage.EXIF));

            if (assetRepository.completeIfAllStagesFinished(event.assetId())) {
                kafkaTemplate.send("job.upload.progress", String.valueOf(event.assetId()),
                        UploadProgressMessage.done(event.assetId()));
            }
        } catch (Exception e) {
            log.error("EXIF processing failed for assetId={}", event.assetId(), e);
            throw new RuntimeException("EXIF processing failed for assetId=" + event.assetId(), e);
        }
    }
}
