package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

public record AssetSummaryResponseDto(Long assetId, String fileName, String folderPath, String thumbnailUrl, long fileSize) {}
