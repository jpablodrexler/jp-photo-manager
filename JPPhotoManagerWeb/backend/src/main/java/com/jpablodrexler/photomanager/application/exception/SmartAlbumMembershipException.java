package com.jpablodrexler.photomanager.application.exception;

public class SmartAlbumMembershipException extends RuntimeException {
    public SmartAlbumMembershipException(String action) {
        super("Cannot manually " + action + " assets to/from a smart album");
    }
}
