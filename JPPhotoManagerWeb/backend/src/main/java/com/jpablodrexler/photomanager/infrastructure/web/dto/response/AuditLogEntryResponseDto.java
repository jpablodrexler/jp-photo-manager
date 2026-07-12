package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntryResponseDto(
        UUID userId,
        String action,
        String entityType,
        String entityId,
        Instant timestamp,
        Map<String, Object> metadata
) {}
