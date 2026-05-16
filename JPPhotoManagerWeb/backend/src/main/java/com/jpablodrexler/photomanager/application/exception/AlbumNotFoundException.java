package com.jpablodrexler.photomanager.application.exception;

public class AlbumNotFoundException extends RuntimeException {
    public AlbumNotFoundException(Long albumId) {
        super("Album not found: " + albumId);
    }
}
