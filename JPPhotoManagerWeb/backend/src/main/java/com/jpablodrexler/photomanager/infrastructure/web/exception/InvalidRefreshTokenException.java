package com.jpablodrexler.photomanager.infrastructure.web.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired, or revoked");
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
