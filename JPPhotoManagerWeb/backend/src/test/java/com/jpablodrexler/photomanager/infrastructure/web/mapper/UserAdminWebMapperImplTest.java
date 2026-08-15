package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.UserSummaryResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAdminWebMapperImplTest {

    private final UserAdminWebMapper sut = new UserAdminWebMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        UserSummary summary = new UserSummary(id, "alice", createdAt);

        UserSummaryResponseDto result = sut.toDto(summary);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toDto_nullSummary_returnsNull() {
        assertThat(sut.toDto(null)).isNull();
    }

    @Test
    void toDtoList_mapsEachElement() {
        UserSummary summary = new UserSummary(UUID.randomUUID(), "bob", Instant.now());

        List<UserSummaryResponseDto> result = sut.toDtoList(List.of(summary));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("bob");
    }

    @Test
    void toDtoList_nullList_returnsNull() {
        assertThat(sut.toDtoList(null)).isNull();
    }
}
