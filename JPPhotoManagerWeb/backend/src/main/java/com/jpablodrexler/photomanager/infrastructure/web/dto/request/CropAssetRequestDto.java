package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

public record CropAssetRequestDto(String formatKey, int x, int y, int width, int height) {}
