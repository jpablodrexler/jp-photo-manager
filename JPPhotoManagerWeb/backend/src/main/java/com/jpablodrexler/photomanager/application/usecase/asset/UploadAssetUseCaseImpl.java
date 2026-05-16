package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.service.CatalogFolderService;
import lombok.RequiredArgsConstructor;
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
    private final CatalogFolderService catalogFolderService;

    @Override
    @Transactional
    public Asset execute(String folderPath, String fileName, byte[] content) throws IOException {
        if (!folderRepository.existsByPath(folderPath)) {
            throw new FolderNotFoundException(folderPath);
        }
        Path tempFile = Files.createTempFile(UUID.randomUUID() + "_", "_" + fileName);
        try {
            Files.write(tempFile, content);
            String destPath = folderPath + "/" + fileName;
            storagePort.copyFile(tempFile.toString(), destPath);
            return catalogFolderService.createAsset(folderPath, fileName);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
