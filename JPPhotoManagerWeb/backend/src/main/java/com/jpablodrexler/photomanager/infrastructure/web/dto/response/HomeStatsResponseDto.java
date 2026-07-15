package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import com.jpablodrexler.photomanager.domain.model.FolderStat;

import java.time.Instant;
import java.util.List;

public record HomeStatsResponseDto(
        long folderCount,
        long assetCount,
        Instant lastCatalogCompletedAt,
        long totalFileSize,
        long duplicateCount,
        List<FolderStat> topFolders,
        List<AssetSummaryResponseDto> recentAssets) {
}
