package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogFilter(
        UUID userId,
        String entityId,
        Instant from,
        Instant to,
        int page,
        int pageSize
) {}
