package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;

public record AssetDeletedEvent(Long assetId, String folderPath, Instant timestamp, boolean permanent) {
}
