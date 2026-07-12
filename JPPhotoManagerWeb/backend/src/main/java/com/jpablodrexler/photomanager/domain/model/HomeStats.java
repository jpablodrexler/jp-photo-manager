package com.jpablodrexler.photomanager.domain.model;

import java.time.Instant;
import java.util.List;

public record HomeStats(
        long folderCount,
        long assetCount,
        Instant lastCatalogCompletedAt,
        long totalFileSize,
        long duplicateCount,
        List<FolderStat> topFolders,
        List<AssetSummaryDto> recentAssets) {
}
