package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;

public record AssetCatalogedEvent(Long assetId, String folderPath, Instant timestamp) {
}
