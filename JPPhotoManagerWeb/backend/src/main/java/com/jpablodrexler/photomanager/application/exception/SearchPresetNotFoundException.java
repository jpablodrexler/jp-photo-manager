package com.jpablodrexler.photomanager.application.exception;

public class SearchPresetNotFoundException extends RuntimeException {
    public SearchPresetNotFoundException(Long presetId) {
        super("Search preset not found: " + presetId);
    }
}
