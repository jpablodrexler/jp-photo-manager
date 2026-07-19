package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

public record ErrorResponseDto(String timestamp, int status, String error, String message) {
}
