package com.jpablodrexler.photomanager.application.dto;

public record AssetSummaryDto(Long assetId, String fileName, String folderPath, String thumbnailUrl, long fileSize) {
}
