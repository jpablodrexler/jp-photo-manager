package com.jpablodrexler.photomanager.domain.model;

public record AssetSummary(Long assetId, String fileName, String folderPath, String thumbnailUrl, long fileSize) {
}
