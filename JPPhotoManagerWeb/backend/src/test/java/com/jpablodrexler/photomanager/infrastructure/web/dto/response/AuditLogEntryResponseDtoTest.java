package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogEntryResponseDtoTest {

    @Test
    void construct_exposesAllFieldsViaAccessors() {
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        Map<String, Object> metadata = Map.of("rating", 5);

        AuditLogEntryResponseDto dto = new AuditLogEntryResponseDto(
                userId, "ASSET_RATED", "ASSET", "42", timestamp, metadata);

        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.action()).isEqualTo("ASSET_RATED");
        assertThat(dto.entityType()).isEqualTo("ASSET");
        assertThat(dto.entityId()).isEqualTo("42");
        assertThat(dto.timestamp()).isEqualTo(timestamp);
        assertThat(dto.metadata()).isEqualTo(metadata);
    }
}
