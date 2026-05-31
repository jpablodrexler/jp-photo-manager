package com.jpablodrexler.photomanager.domain.model;

public record CropAssetRequest(String formatKey, int x, int y, int width, int height) {}
