package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single append-only audit trail entry. {@code metadata} is a free-form map whose shape
 * varies per {@link #action}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private UUID userId;
    private AuditAction action;
    private AuditEntityType entityType;
    private String entityId;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
