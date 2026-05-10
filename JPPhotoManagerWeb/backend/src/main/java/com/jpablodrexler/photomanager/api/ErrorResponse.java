package com.jpablodrexler.photomanager.api;

public record ErrorResponse(String timestamp, int status, String error, String message) {
}
