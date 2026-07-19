package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.application.exception.UnsupportedAssetTypeException;
import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAssetUseCaseImpl implements UploadAssetUseCase {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp");

    private final FolderRepository folderRepository;
    private final StoragePort storagePort;
    private final AssetRepository assetRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Asset execute(String folderPath, String originalFilename, String contentType, byte[] content) throws IOException {
        String fileName = sanitizeAndValidate(originalFilename, contentType);

        Folder folder = folderRepository.findByPath(folderPath)
                .orElseThrow(() -> new FolderNotFoundException(folderPath));

        Path tempFile = Files.createTempFile(UUID.randomUUID() + "_", "_" + fileName);
        try {
            Files.write(tempFile, content);
            String destPath = folderPath + "/" + fileName;
            storagePort.copyFile(tempFile.toString(), destPath);

            Asset asset = new Asset();
            asset.setFolder(folder);
            asset.setFileName(fileName);
            asset.setFileSize(storagePort.getFileSize(destPath));
            asset.setFileCreationDateTime(storagePort.getFileCreationDateTime(destPath));
            asset.setFileModificationDateTime(storagePort.getFileModificationDateTime(destPath));
            asset.setVideo(storagePort.isVideoFile(fileName));
            asset.setFileType(asset.isVideo() ? FileType.VIDEO : FileType.IMAGE);
            asset.setProcessingStatus(ProcessingStatus.PENDING);
            asset.setHash(null);
            asset.setThumbnailCreationDateTime(null);

            asset = assetRepository.save(asset);

            AssetUploadedEvent event = new AssetUploadedEvent(asset.getAssetId(), destPath, folderPath, fileName);
            // Deferred until commit: a consumer that reads asset.uploaded before this transaction
            // commits would race against the placeholder row's visibility.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send("asset.uploaded", String.valueOf(event.assetId()), event);
                }
            });

            return asset;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String sanitizeAndValidate(String originalFilename, String contentType) {
        // Strip any directory prefix the client may have embedded in the filename (path traversal defence).
        // Paths.get().getFileName() returns only the last component; replacing '\' first handles Windows paths
        // sent by any client regardless of the server's OS.
        Path filenamePath = originalFilename != null
                ? Paths.get(originalFilename.replace('\\', '/')).getFileName()
                : null;
        String safeFilename = (filenamePath != null) ? filenamePath.toString() : null;
        if (safeFilename == null || safeFilename.isBlank()) {
            throw new IllegalArgumentException("Missing or invalid file name");
        }
        String extension = safeFilename.contains(".")
                ? safeFilename.substring(safeFilename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)
                || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedAssetTypeException(contentType);
        }
        return safeFilename;
    }
}
