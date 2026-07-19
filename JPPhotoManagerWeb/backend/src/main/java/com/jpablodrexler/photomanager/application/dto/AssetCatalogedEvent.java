package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetCatalogedEvent(Long assetId, String folderPath, Instant timestamp, UUID userId) {
}
