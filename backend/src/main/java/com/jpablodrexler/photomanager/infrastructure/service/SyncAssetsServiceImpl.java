package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.repository.SyncAssetsConfigRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import com.jpablodrexler.photomanager.domain.service.SyncAssetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncAssetsServiceImpl implements SyncAssetsService {

    private final SyncAssetsConfigRepository configRepository;
    private final StorageService storageService;

    @Async
    @Override
    public CompletableFuture<List<SyncAssetsResult>> executeAsync(Consumer<String> statusCallback) {
        List<SyncAssetsDirectoriesDefinition> definitions = configRepository.findAllByOrderByOrderAsc();
        List<SyncAssetsResult> results = new ArrayList<>();

        for (SyncAssetsDirectoriesDefinition def : definitions) {
            SyncAssetsResult result = syncDirectories(def, statusCallback);
            results.add(result);
        }

        return CompletableFuture.completedFuture(results);
    }

    private SyncAssetsResult syncDirectories(SyncAssetsDirectoriesDefinition def, Consumer<String> statusCallback) {
        SyncAssetsResult result = new SyncAssetsResult(def.getSourceDirectory(), def.getDestinationDirectory());

        if (!storageService.directoryExists(def.getSourceDirectory())) {
            result.setMessage("Source directory does not exist: " + def.getSourceDirectory());
            result.setSuccess(false);
            return result;
        }

        if (!storageService.directoryExists(def.getDestinationDirectory())) {
            storageService.createDirectory(def.getDestinationDirectory());
        }

        try {
            syncFolder(def.getSourceDirectory(), def.getDestinationDirectory(),
                    def.isIncludeSubFolders(), def.isDeleteAssetsNotInSource(), result, statusCallback);
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Sync failed from {} to {}", def.getSourceDirectory(), def.getDestinationDirectory(), e);
            result.setMessage("Error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    private void syncFolder(String sourceDir, String destDir, boolean includeSubFolders,
                             boolean deleteNotInSource, SyncAssetsResult result,
                             Consumer<String> statusCallback) throws IOException {
        List<String> sourceFiles = storageService.listFiles(sourceDir);
        List<String> destFiles = storageService.listFiles(destDir);

        Set<String> destFileNames = new HashSet<>();
        for (String f : destFiles) {
            destFileNames.add(Paths.get(f).getFileName().toString());
        }

        Set<String> sourceFileNames = new HashSet<>();
        for (String f : sourceFiles) {
            String fileName = Paths.get(f).getFileName().toString();
            sourceFileNames.add(fileName);

            if (!destFileNames.contains(fileName)) {
                String destPath = destDir + "/" + fileName;
                storageService.copyFile(f, destPath);
                result.setSyncedCount(result.getSyncedCount() + 1);
                if (statusCallback != null) {
                    statusCallback.accept("Copied: " + fileName);
                }
            }
        }

        if (deleteNotInSource) {
            for (String f : destFiles) {
                String fileName = Paths.get(f).getFileName().toString();
                if (!sourceFileNames.contains(fileName)) {
                    storageService.deleteFile(f);
                    result.setDeletedCount(result.getDeletedCount() + 1);
                    if (statusCallback != null) {
                        statusCallback.accept("Deleted: " + fileName);
                    }
                }
            }
        }

        if (includeSubFolders) {
            for (String subDir : storageService.listSubDirectories(sourceDir)) {
                String subDirName = Paths.get(subDir).getFileName().toString();
                String destSubDir = destDir + "/" + subDirName;
                syncFolder(subDir, destSubDir, true, deleteNotInSource, result, statusCallback);
            }
        }
    }
}
