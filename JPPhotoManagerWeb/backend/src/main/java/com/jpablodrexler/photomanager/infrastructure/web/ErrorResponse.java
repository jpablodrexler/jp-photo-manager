package com.jpablodrexler.photomanager.infrastructure.web;

public record ErrorResponse(String timestamp, int status, String error, String message) {
}
