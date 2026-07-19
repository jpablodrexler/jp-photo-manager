package com.jpablodrexler.photomanager.application.exception;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String name) {
        super("Tag not found: " + name);
    }
}
