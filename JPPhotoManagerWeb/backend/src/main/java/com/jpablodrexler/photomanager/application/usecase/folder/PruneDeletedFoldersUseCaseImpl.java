package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.Reason;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.PruneDeletedFoldersUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    @Override
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
        List<String> thumbnailBlobNames = assets.stream().map(Asset::getThumbnailBlobName).toList();

        // Delete and commit the DB records first so a mid-batch failure never leaves a DB
        // row pointing at an already-deleted thumbnail; the thumbnail files are removed only
        // after the DB transaction has committed successfully.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (Asset asset : assets) {
                assetRepository.deleteById(asset.getAssetId());
            }
            folderRepository.deleteById(folder.getFolderId());
        });

        for (String blobName : thumbnailBlobNames) {
            thumbnailPort.deleteThumbnail(blobName);
        }

        log.info("Pruned folder no longer on disk: {}", folder.getPath());
        if (consumer != null) {
            consumer.accept(new CatalogChangeNotification(Reason.FOLDER_DELETED, folder.getPath(), 0));
        }
    }
}
