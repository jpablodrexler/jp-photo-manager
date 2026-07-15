package com.jpablodrexler.photomanager.domain.model;

public record AssetImage(byte[] bytes, String fileName, String mimeType) {
}
