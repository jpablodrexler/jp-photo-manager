package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AuditLogEntryResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogWebMapperImplTest {

    private final AuditLogWebMapper sut = new AuditLogWebMapperImpl();

    @Test
    void toDto_validEvent_mapsEnumsToNamesAndCopiesMetadata() {
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-05T12:00:00Z");
        Map<String, Object> metadata = Map.of("rating", 4);

        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .action(AuditAction.ASSET_TAGGED)
                .entityType(AuditEntityType.ASSET)
                .entityId("55")
                .timestamp(timestamp)
                .metadata(metadata)
                .build();

        AuditLogEntryResponseDto result = sut.toDto(event);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.action()).isEqualTo("ASSET_TAGGED");
        assertThat(result.entityType()).isEqualTo("ASSET");
        assertThat(result.entityId()).isEqualTo("55");
        assertThat(result.timestamp()).isEqualTo(timestamp);
        assertThat(result.metadata()).isEqualTo(metadata);
    }

    @Test
    void toDto_eventWithNullActionAndEntityType_leavesThemNull() {
        AuditEvent event = AuditEvent.builder()
                .userId(UUID.randomUUID())
                .entityId("1")
                .build();

        AuditLogEntryResponseDto result = sut.toDto(event);

        assertThat(result.action()).isNull();
        assertThat(result.entityType()).isNull();
    }

    @Test
    void toDto_nullEvent_returnsNull() {
        assertThat(sut.toDto(null)).isNull();
    }
}
