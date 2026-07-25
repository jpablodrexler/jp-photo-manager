package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;

public record SessionResponseDto(long id, String deviceHint, Instant lastUsedAt, boolean current) {
}
