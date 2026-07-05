package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAssetUseCaseImpl implements UploadAssetUseCase {

    private final FolderRepository folderRepository;
    private final StoragePort storagePort;
    private final AssetRepository assetRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Asset execute(String folderPath, String fileName, byte[] content) throws IOException {
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
            kafkaTemplate.send("asset.uploaded", String.valueOf(asset.getAssetId()), event);

            return asset;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
