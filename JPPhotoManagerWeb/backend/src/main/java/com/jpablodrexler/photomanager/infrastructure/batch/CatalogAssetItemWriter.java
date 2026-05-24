package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.service.SseNotificationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class CatalogAssetItemWriter implements ItemWriter<CatalogBatchItem>, StepExecutionListener {

    private final long runId;
    private final String folderPath;
    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final AssetAudioRepository assetAudioRepository;
    private final FolderRepository folderRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;
    private final SseNotificationRegistry sseNotificationRegistry;

    private Folder cachedFolder;

    public CatalogAssetItemWriter(long runId, String folderPath,
                                   AssetRepository assetRepository,
                                   AssetExifRepository assetExifRepository,
                                   AssetAudioRepository assetAudioRepository,
                                   FolderRepository folderRepository,
                                   StoragePort storagePort,
                                   ThumbnailPort thumbnailPort,
                                   SseNotificationRegistry sseNotificationRegistry) {
        this.runId = runId;
        this.folderPath = folderPath;
        this.assetRepository = assetRepository;
        this.assetExifRepository = assetExifRepository;
        this.assetAudioRepository = assetAudioRepository;
        this.folderRepository = folderRepository;
        this.storagePort = storagePort;
        this.thumbnailPort = thumbnailPort;
        this.sseNotificationRegistry = sseNotificationRegistry;
    }

    @Override
    public void write(Chunk<? extends CatalogBatchItem> chunk) throws Exception {
        ensureFolderExists();
        Consumer<CatalogChangeNotification> consumer = sseNotificationRegistry.get(runId);

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

            if (consumer != null) {
                consumer.accept(new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, saved, 0));
            }
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
            Consumer<CatalogChangeNotification> consumer = sseNotificationRegistry.get(runId);

            for (Asset asset : cataloguedAssets) {
                if (!filesOnDisk.contains(asset.getFileName())) {
                    assetRepository.deleteById(asset.getAssetId());
                    thumbnailPort.deleteThumbnail(asset.getThumbnailBlobName());
                    if (consumer != null) {
                        consumer.accept(new CatalogChangeNotification(ReasonEnum.ASSET_DELETED, asset, 0));
                    }
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
                Consumer<CatalogChangeNotification> consumer = sseNotificationRegistry.get(runId);
                if (consumer != null) {
                    consumer.accept(new CatalogChangeNotification(ReasonEnum.FOLDER_CREATED, folderPath, 0));
                }
                return saved;
            });
        }
    }
}
