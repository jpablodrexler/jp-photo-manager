package com.jpablodrexler.photomanager.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component("thumbnailStorage")
@Slf4j
public class ThumbnailStorageHealthIndicator implements HealthIndicator {

    private final Path thumbnailsDir;

    public ThumbnailStorageHealthIndicator(
            @Value("${photomanager.thumbnails-directory}") String thumbnailsDirectory) {
        this.thumbnailsDir = Paths.get(thumbnailsDirectory);
    }

    @Override
    public Health health() {
        if (!Files.isDirectory(thumbnailsDir)) {
            return Health.down()
                    .withDetail("path", thumbnailsDir.toString())
                    .withDetail("reason", "directory does not exist")
                    .build();
        }
        if (!Files.isWritable(thumbnailsDir)) {
            return Health.down()
                    .withDetail("path", thumbnailsDir.toString())
                    .withDetail("reason", "directory is not writable")
                    .build();
        }
        return Health.up().withDetail("path", thumbnailsDir.toString()).build();
    }
}
