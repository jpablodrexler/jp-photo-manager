package com.jpablodrexler.photomanager.domain.model;

public record AssetSummaryDto(Long assetId, String fileName, String folderPath, String thumbnailUrl, long fileSize) {
}
