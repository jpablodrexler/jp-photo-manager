package com.jpablodrexler.photomanager.application.exception;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(Long assetId) {
        super("Asset not found: " + assetId);
    }
}
