package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogDocumentMapperImplTest {

    private final AuditLogDocumentMapper sut = new AuditLogDocumentMapperImpl();

    @Test
    void toDocument_validEvent_mapsAllFieldsAndIgnoresId() {
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        Map<String, Object> metadata = Map.of("rating", 5);

        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .action(AuditAction.ASSET_RATED)
                .entityType(AuditEntityType.ASSET)
                .entityId("42")
                .timestamp(timestamp)
                .metadata(metadata)
                .build();

        AuditLogDocument result = sut.toDocument(event);

        assertThat(result.getId()).isNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAction()).isEqualTo(AuditAction.ASSET_RATED);
        assertThat(result.getEntityType()).isEqualTo(AuditEntityType.ASSET);
        assertThat(result.getEntityId()).isEqualTo("42");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getMetadata()).isEqualTo(metadata);
    }

    @Test
    void toDocument_nullEvent_returnsNull() {
        assertThat(sut.toDocument(null)).isNull();
    }

    @Test
    void toDomain_validDocument_mapsAllFields() {
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-02-01T00:00:00Z");
        Map<String, Object> metadata = Map.of("count", 3);

        AuditLogDocument document = AuditLogDocument.builder()
                .id("mongo-id-1")
                .userId(userId)
                .action(AuditAction.CATALOG_RUN)
                .entityType(AuditEntityType.CATALOG_RUN)
                .entityId("run-1")
                .timestamp(timestamp)
                .metadata(metadata)
                .build();

        AuditEvent result = sut.toDomain(document);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAction()).isEqualTo(AuditAction.CATALOG_RUN);
        assertThat(result.getEntityType()).isEqualTo(AuditEntityType.CATALOG_RUN);
        assertThat(result.getEntityId()).isEqualTo("run-1");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getMetadata()).isEqualTo(metadata);
    }

    @Test
    void toDomain_nullDocument_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }
}
