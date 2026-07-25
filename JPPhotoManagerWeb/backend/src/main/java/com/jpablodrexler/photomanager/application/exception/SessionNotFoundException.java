package com.jpablodrexler.photomanager.application.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(long sessionId) {
        super("Session not found: " + sessionId);
    }
}
