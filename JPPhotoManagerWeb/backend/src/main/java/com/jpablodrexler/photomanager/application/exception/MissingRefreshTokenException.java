package com.jpablodrexler.photomanager.application.exception;

public class MissingRefreshTokenException extends RuntimeException {
    public MissingRefreshTokenException() {
        super("Refresh token cookie is missing");
    }
}
