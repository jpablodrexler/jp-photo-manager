package com.jpablodrexler.photomanager.api.exception;

public class SearchPresetNotFoundException extends RuntimeException {

    public SearchPresetNotFoundException(Long presetId) {
        super("Search preset not found: " + presetId);
    }
}
