package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponseDto(UUID id, String username, Instant createdAt) {
}
