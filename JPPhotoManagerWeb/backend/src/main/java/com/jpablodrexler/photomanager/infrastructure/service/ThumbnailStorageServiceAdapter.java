package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ThumbnailStorageServiceAdapter implements ThumbnailPort {

    @Value("${photomanager.thumbnails-directory:${user.home}/.photomanager/thumbnails}")
    private String thumbnailsDirectory;

    @Override
    public void saveThumbnail(String blobName, byte[] data) {
        try {
            Path dir = Paths.get(thumbnailsDirectory);
            Files.createDirectories(dir);
            Files.write(dir.resolve(blobName), data);
        } catch (IOException e) {
            log.error("Failed to save thumbnail {}", blobName, e);
        }
    }

    @Override
    public byte[] loadThumbnail(String blobName) {
        try {
            Path path = Paths.get(thumbnailsDirectory).resolve(blobName);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            log.error("Failed to load thumbnail {}", blobName, e);
        }
        return null;
    }

    @Override
    public void deleteThumbnail(String blobName) {
        try {
            Files.deleteIfExists(Paths.get(thumbnailsDirectory).resolve(blobName));
        } catch (IOException e) {
            log.error("Failed to delete thumbnail {}", blobName, e);
        }
    }

    @Override
    public boolean thumbnailExists(String blobName) {
        return Files.exists(Paths.get(thumbnailsDirectory).resolve(blobName));
    }
}
