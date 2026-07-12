package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.catalog.PruneDeletedFoldersUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class PruneDeletedFoldersUseCaseImpl implements PruneDeletedFoldersUseCase {

    private final FolderRepository folderRepository;
    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;

    @Override
    @Transactional
    public void execute(Consumer<CatalogChangeNotification> consumer) {
        List<Folder> allFolders = folderRepository.findAll();
        for (Folder folder : allFolders) {
            if (!storagePort.directoryExists(folder.getPath())) {
                pruneFolder(folder, consumer);
            }
        }
    }

    private void pruneFolder(Folder folder, Consumer<CatalogChangeNotification> consumer) {
        List<Asset> assets = assetRepository.findByFolder(folder);
        for (Asset asset : assets) {
            thumbnailPort.deleteThumbnail(asset.getThumbnailBlobName());
            assetRepository.deleteById(asset.getAssetId());
        }
        folderRepository.deleteById(folder.getFolderId());
        log.info("Pruned folder no longer on disk: {}", folder.getPath());
        if (consumer != null) {
            consumer.accept(new CatalogChangeNotification(ReasonEnum.FOLDER_DELETED, folder.getPath(), 0));
        }
    }
}
