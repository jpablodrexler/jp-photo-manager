package com.jpablodrexler.photomanager.api.exception;

public class AlbumNotFoundException extends RuntimeException {
    public AlbumNotFoundException(Long albumId) {
        super("Album not found: " + albumId);
    }
}
