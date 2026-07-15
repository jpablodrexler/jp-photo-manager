package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreatePresetRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SearchPresetResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SearchPresetWebMapperTest {

    private final SearchPresetWebMapper sut =
            new SearchPresetWebMapperImpl(new SearchPresetFilterJsonConverter(new ObjectMapper()));

    @Test
    void toDomain_mapsAllFieldsFromRequestDto() {
        CreatePresetRequestDto dto = new CreatePresetRequestDto("Sunsets", "sunset", "2024-01-01", "2024-12-31", 4);

        FilterPreset result = sut.toDomain(dto);

        assertThat(result.search()).isEqualTo("sunset");
        assertThat(result.dateFrom()).isEqualTo("2024-01-01");
        assertThat(result.dateTo()).isEqualTo("2024-12-31");
        assertThat(result.minRating()).isEqualTo(4);
    }

    @Test
    void toDto_validFilterJson_flattensFilterFieldsIntoResponseDto() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        SearchPreset preset = SearchPreset.builder()
                .presetId(7L)
                .name("Sunsets")
                .createdAt(createdAt)
                .filterJson("{\"search\":\"sunset\",\"dateFrom\":\"2024-01-01\",\"dateTo\":\"2024-12-31\",\"minRating\":4}")
                .build();

        SearchPresetResponseDto result = sut.toDto(preset);

        assertThat(result.presetId()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Sunsets");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.search()).isEqualTo("sunset");
        assertThat(result.dateFrom()).isEqualTo("2024-01-01");
        assertThat(result.dateTo()).isEqualTo("2024-12-31");
        assertThat(result.minRating()).isEqualTo(4);
    }

    @Test
    void toDto_blankFilterJson_leavesFilterFieldsNull() {
        SearchPreset preset = SearchPreset.builder()
                .presetId(8L)
                .name("Empty")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .filterJson("")
                .build();

        SearchPresetResponseDto result = sut.toDto(preset);

        assertThat(result.search()).isNull();
        assertThat(result.dateFrom()).isNull();
        assertThat(result.dateTo()).isNull();
        assertThat(result.minRating()).isNull();
    }
}
