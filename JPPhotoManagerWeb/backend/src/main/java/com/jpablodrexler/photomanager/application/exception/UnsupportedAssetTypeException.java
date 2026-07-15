package com.jpablodrexler.photomanager.application.exception;

public class UnsupportedAssetTypeException extends RuntimeException {
    public UnsupportedAssetTypeException(String contentType) {
        super("Unsupported asset content type: " + contentType);
    }
}
