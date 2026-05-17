package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncAssetsUseCaseImpl implements SyncAssetsUseCase {

    private final SyncConfigRepository syncConfigRepository;
    private final StoragePort storagePort;

    @Async
    @Override
    public CompletableFuture<List<SyncAssetsResult>> execute(Consumer<String> listener) {
        List<SyncDirectoriesDefinition> definitions = syncConfigRepository.findAllOrderByOrder();
        List<SyncAssetsResult> results = new ArrayList<>();

        for (SyncDirectoriesDefinition def : definitions) {
            results.add(syncDirectories(def, listener));
        }

        return CompletableFuture.completedFuture(results);
    }

    private SyncAssetsResult syncDirectories(SyncDirectoriesDefinition def, Consumer<String> statusCallback) {
        SyncAssetsResult result = new SyncAssetsResult(def.getSourceDirectory(), def.getDestinationDirectory());

        if (!storagePort.directoryExists(def.getSourceDirectory())) {
            result.setMessage("Source directory does not exist: " + def.getSourceDirectory());
            result.setSuccess(false);
            return result;
        }

        if (!storagePort.directoryExists(def.getDestinationDirectory())) {
            storagePort.createDirectory(def.getDestinationDirectory());
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
        List<String> sourceFiles = storagePort.listFiles(sourceDir);
        List<String> destFiles = storagePort.listFiles(destDir);

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
                storagePort.copyFile(f, destPath);
                result.setSyncedCount(result.getSyncedCount() + 1);
                if (statusCallback != null) statusCallback.accept("Copied: " + fileName);
            }
        }

        if (deleteNotInSource) {
            for (String f : destFiles) {
                String fileName = Paths.get(f).getFileName().toString();
                if (!sourceFileNames.contains(fileName)) {
                    storagePort.deleteFile(f);
                    result.setDeletedCount(result.getDeletedCount() + 1);
                    if (statusCallback != null) statusCallback.accept("Deleted: " + fileName);
                }
            }
        }

        if (includeSubFolders) {
            for (String subDir : storagePort.listSubDirectories(sourceDir)) {
                String subDirName = Paths.get(subDir).getFileName().toString();
                syncFolder(subDir, destDir + "/" + subDirName, true, deleteNotInSource, result, statusCallback);
            }
        }
    }
}
