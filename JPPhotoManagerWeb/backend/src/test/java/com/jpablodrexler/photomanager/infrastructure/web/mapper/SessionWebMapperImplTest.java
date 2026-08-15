package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.SessionInfo;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SessionResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionWebMapperImplTest {

    private final SessionWebMapper sut = new SessionWebMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        Instant lastUsedAt = Instant.parse("2026-01-10T00:00:00Z");
        SessionInfo sessionInfo = new SessionInfo(7L, "Chrome on Windows", lastUsedAt, true);

        SessionResponseDto result = sut.toDto(sessionInfo);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.deviceHint()).isEqualTo("Chrome on Windows");
        assertThat(result.lastUsedAt()).isEqualTo(lastUsedAt);
        assertThat(result.current()).isTrue();
    }

    @Test
    void toDto_nullSessionInfo_returnsNull() {
        assertThat(sut.toDto(null)).isNull();
    }

    @Test
    void toDtoList_mapsEachElement() {
        SessionInfo sessionInfo = new SessionInfo(1L, "Firefox", Instant.now(), false);

        List<SessionResponseDto> result = sut.toDtoList(List.of(sessionInfo));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void toDtoList_nullList_returnsNull() {
        assertThat(sut.toDtoList(null)).isNull();
    }
}
