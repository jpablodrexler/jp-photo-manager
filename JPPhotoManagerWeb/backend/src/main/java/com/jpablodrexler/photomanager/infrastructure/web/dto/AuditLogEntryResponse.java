package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID userId,
        String action,
        String entityType,
        String entityId,
        Instant timestamp,
        Map<String, Object> metadata
) {}
