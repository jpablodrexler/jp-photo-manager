package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.repository.ConvertAssetsConfigRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import com.jpablodrexler.photomanager.domain.service.ConvertAssetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConvertAssetsServiceImpl implements ConvertAssetsService {

    private final ConvertAssetsConfigRepository configRepository;
    private final StorageService storageService;

    @Async
    @Override
    public CompletableFuture<List<ConvertAssetsResult>> executeAsync(Consumer<String> statusCallback) {
        List<ConvertAssetsDirectoriesDefinition> definitions = configRepository.findAllByOrderByOrderAsc();
        List<ConvertAssetsResult> results = new ArrayList<>();

        for (ConvertAssetsDirectoriesDefinition def : definitions) {
            ConvertAssetsResult result = convertDirectory(def, statusCallback);
            results.add(result);
        }

        return CompletableFuture.completedFuture(results);
    }

    private ConvertAssetsResult convertDirectory(ConvertAssetsDirectoriesDefinition def,
                                                  Consumer<String> statusCallback) {
        ConvertAssetsResult result = new ConvertAssetsResult(def.getSourceDirectory(), def.getDestinationDirectory());

        if (!storageService.directoryExists(def.getSourceDirectory())) {
            result.setMessage("Source directory does not exist: " + def.getSourceDirectory());
            result.setSuccess(false);
            return result;
        }

        if (!storageService.directoryExists(def.getDestinationDirectory())) {
            storageService.createDirectory(def.getDestinationDirectory());
        }

        List<String> sourceFiles = storageService.listFiles(def.getSourceDirectory());
        for (String filePath : sourceFiles) {
            if (filePath.toLowerCase().endsWith(".png")) {
                String fileName = Paths.get(filePath).getFileName().toString();
                String destFileName = fileName.replaceAll("(?i)\\.png$", ".jpg");
                String destPath = def.getDestinationDirectory() + "/" + destFileName;

                try {
                    convertPngToJpeg(filePath, destPath);
                    result.setConvertedCount(result.getConvertedCount() + 1);
                    if (statusCallback != null) {
                        statusCallback.accept("Converted: " + fileName + " -> " + destFileName);
                    }
                } catch (IOException e) {
                    log.error("Failed to convert {}", filePath, e);
                    result.setFailedCount(result.getFailedCount() + 1);
                }
            }
        }

        result.setSuccess(true);
        return result;
    }

    private void convertPngToJpeg(String sourcePath, String destPath) throws IOException {
        BufferedImage image = storageService.loadImage(sourcePath);
        // Convert transparent PNG to white background JPEG
        BufferedImage jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        jpegImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(jpegImage, "JPEG", baos);
        Files.write(Paths.get(destPath), baos.toByteArray());
    }
}
