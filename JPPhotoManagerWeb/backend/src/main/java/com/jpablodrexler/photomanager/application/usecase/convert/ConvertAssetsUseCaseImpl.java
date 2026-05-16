package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.convert.ConvertAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConvertAssetsUseCaseImpl implements ConvertAssetsUseCase {

    private final ConvertConfigRepository convertConfigRepository;
    private final StoragePort storagePort;

    @Async
    @Override
    public CompletableFuture<List<ConvertAssetsResult>> execute(Consumer<String> listener) {
        List<ConvertDirectoriesDefinition> definitions = convertConfigRepository.findAllOrderByOrder();
        List<ConvertAssetsResult> results = new ArrayList<>();

        for (ConvertDirectoriesDefinition def : definitions) {
            results.add(convertDirectory(def, listener));
        }

        return CompletableFuture.completedFuture(results);
    }

    private ConvertAssetsResult convertDirectory(ConvertDirectoriesDefinition def, Consumer<String> statusCallback) {
        ConvertAssetsResult result = new ConvertAssetsResult(def.getSourceDirectory(), def.getDestinationDirectory());

        if (!storagePort.directoryExists(def.getSourceDirectory())) {
            result.setMessage("Source directory does not exist: " + def.getSourceDirectory());
            result.setSuccess(false);
            return result;
        }

        if (!storagePort.directoryExists(def.getDestinationDirectory())) {
            storagePort.createDirectory(def.getDestinationDirectory());
        }

        List<String> sourceFiles = storagePort.listFiles(def.getSourceDirectory());
        for (String filePath : sourceFiles) {
            if (filePath.toLowerCase().endsWith(".png")) {
                String fileName = Paths.get(filePath).getFileName().toString();
                String destFileName = fileName.replaceAll("(?i)\\.png$", ".jpg");
                String destPath = def.getDestinationDirectory() + "/" + destFileName;
                try {
                    storagePort.convertPngToJpeg(filePath, destPath);
                    result.setConvertedCount(result.getConvertedCount() + 1);
                    if (statusCallback != null) statusCallback.accept("Converted: " + fileName + " -> " + destFileName);
                } catch (IOException e) {
                    log.error("Failed to convert {}", filePath, e);
                    result.setFailedCount(result.getFailedCount() + 1);
                }
            }
        }

        result.setSuccess(true);
        return result;
    }
}
