package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;
import java.util.List;

public record HomeStatsResponseDto(
        long folderCount,
        long assetCount,
        Instant lastCatalogCompletedAt,
        long totalFileSize,
        long duplicateCount,
        List<FolderStatResponseDto> topFolders,
        List<AssetSummaryResponseDto> recentAssets) {
}
