package com.jpablodrexler.photomanager.infrastructure.persistence.document;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit trail entry stored in the {@code asset_audit_log} MongoDB collection. Documents
 * are never updated or deleted through normal application flow; the only removals are automatic
 * expiry via the TTL index registered by {@link com.jpablodrexler.photomanager.infrastructure.config.MongoIndexInitializer}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "asset_audit_log")
public class AuditLogDocument {

    @Id
    private String id;

    private UUID userId;
    private AuditAction action;
    private AuditEntityType entityType;
    private String entityId;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
