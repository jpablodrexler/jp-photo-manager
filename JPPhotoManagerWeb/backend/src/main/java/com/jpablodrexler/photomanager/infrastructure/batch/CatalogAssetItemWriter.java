package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.Reason;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class CatalogAssetItemWriter implements ItemWriter<CatalogBatchItem>, StepExecutionListener {

    private final long runId;
    private final UUID userId;
    private final String folderPath;
    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final AssetAudioRepository assetAudioRepository;
    private final FolderRepository folderRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter catalogAssetsCounter;

    private Folder cachedFolder;

    public CatalogAssetItemWriter(long runId, String userId, String folderPath,
                                   AssetRepository assetRepository,
                                   AssetExifRepository assetExifRepository,
                                   AssetAudioRepository assetAudioRepository,
                                   FolderRepository folderRepository,
                                   StoragePort storagePort,
                                   ThumbnailPort thumbnailPort,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   MeterRegistry meterRegistry) {
        this.runId = runId;
        this.userId = userId != null ? UUID.fromString(userId) : null;
        this.folderPath = folderPath;
        this.assetRepository = assetRepository;
        this.assetExifRepository = assetExifRepository;
        this.assetAudioRepository = assetAudioRepository;
        this.folderRepository = folderRepository;
        this.storagePort = storagePort;
        this.thumbnailPort = thumbnailPort;
        this.kafkaTemplate = kafkaTemplate;
        this.catalogAssetsCounter = Counter.builder("photomanager_catalog_assets_total")
                .description("Total assets cataloged")
                .register(meterRegistry);
    }

    @Override
    public void write(Chunk<? extends CatalogBatchItem> chunk) throws Exception {
        ensureFolderExists();

        for (CatalogBatchItem item : chunk) {
            Asset asset = item.asset();
            asset.setFolder(cachedFolder);
            Asset saved = assetRepository.save(asset);

            thumbnailPort.saveThumbnail(saved.getThumbnailBlobName(), item.thumbnailData());

            if (item.assetExif() != null) {
                AssetExif exif = item.assetExif();
                exif.setAssetId(saved.getAssetId());
                assetExifRepository.save(exif);
            }

            if (item.assetAudio() != null) {
                item.assetAudio().setAssetId(saved.getAssetId());
                assetAudioRepository.save(item.assetAudio());
            }

            catalogAssetsCounter.increment();

            CatalogChangeNotification notification =
                    new CatalogChangeNotification(Reason.ASSET_CREATED, saved, 0);
            safeSend("job.catalog.progress", String.valueOf(runId),
                    CatalogProgressMessage.progress(runId, notification));
            safeSend("asset.cataloged", String.valueOf(saved.getAssetId()),
                    new AssetCatalogedEvent(saved.getAssetId(), folderPath, Instant.now(), userId));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            Folder folder = folderRepository.findByPath(folderPath).orElseGet(() -> {
                Folder f = new Folder();
                f.setPath(folderPath);
                return folderRepository.save(f);
            });

            Set<String> filesOnDisk = storagePort.listFiles(folderPath).stream()
                    .map(p -> Paths.get(p).getFileName().toString())
                    .collect(Collectors.toSet());

            List<Asset> cataloguedAssets = assetRepository.findByFolder(folder);

            for (Asset asset : cataloguedAssets) {
                if (!filesOnDisk.contains(asset.getFileName())) {
                    assetRepository.deleteById(asset.getAssetId());
                    thumbnailPort.deleteThumbnail(asset.getThumbnailBlobName());

                    CatalogChangeNotification notification =
                            new CatalogChangeNotification(Reason.ASSET_DELETED, asset, 0);
                    safeSend("job.catalog.progress", String.valueOf(runId),
                            CatalogProgressMessage.progress(runId, notification));
                    safeSend("asset.deleted", String.valueOf(asset.getAssetId()),
                            new AssetDeletedEvent(asset.getAssetId(), folder.getFolderId(), folderPath, Instant.now(),
                                    false, userId));
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning up stale assets for folder: {}", folderPath, e);
        }
        return stepExecution.getExitStatus();
    }

    private void ensureFolderExists() {
        if (cachedFolder == null) {
            cachedFolder = folderRepository.findByPath(folderPath).orElseGet(() -> {
                Folder f = new Folder();
                f.setPath(folderPath);
                Folder saved = folderRepository.save(f);
                CatalogChangeNotification notification =
                        new CatalogChangeNotification(Reason.FOLDER_CREATED, folderPath, 0);
                safeSend("job.catalog.progress", String.valueOf(runId),
                        CatalogProgressMessage.progress(runId, notification));
                return saved;
            });
        }
    }

    // Progress notifications are best-effort: a Kafka hiccup (broker restart,
    // topic not yet ready) must never fail the batch step and roll back an
    // asset that was already successfully persisted to Postgres.
    private void safeSend(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.warn("Failed to publish {} message (key={}): {}", topic, key, e.getMessage());
        }
    }
}
