package com.jpablodrexler.photomanager.application.dto;

public record AssetUploadedEvent(Long assetId, String filePath, String folderPath, String fileName) {
}
