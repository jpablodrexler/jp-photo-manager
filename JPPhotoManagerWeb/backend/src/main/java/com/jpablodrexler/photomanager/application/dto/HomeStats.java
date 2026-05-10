package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;

public record HomeStats(long folderCount, long assetCount, Instant lastCatalogCompletedAt) {
}
