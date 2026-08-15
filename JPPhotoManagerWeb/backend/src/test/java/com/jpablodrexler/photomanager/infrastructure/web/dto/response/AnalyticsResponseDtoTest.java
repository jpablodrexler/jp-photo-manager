package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsResponseDtoTest {

    @Test
    void folderStorageEntryDto_construct_exposesFieldsViaAccessors() {
        AnalyticsResponseDto.FolderStorageEntryDto dto =
                new AnalyticsResponseDto.FolderStorageEntryDto("/photos", 1024L);

        assertThat(dto.folderPath()).isEqualTo("/photos");
        assertThat(dto.bytes()).isEqualTo(1024L);
    }

    @Test
    void formatEntryDto_construct_exposesFieldsViaAccessors() {
        AnalyticsResponseDto.FormatEntryDto dto = new AnalyticsResponseDto.FormatEntryDto("jpg", 5L);

        assertThat(dto.extension()).isEqualTo("jpg");
        assertThat(dto.count()).isEqualTo(5L);
    }

    @Test
    void monthlyCountEntryDto_construct_exposesFieldsViaAccessors() {
        AnalyticsResponseDto.MonthlyCountEntryDto dto = new AnalyticsResponseDto.MonthlyCountEntryDto("2026-01", 7L);

        assertThat(dto.month()).isEqualTo("2026-01");
        assertThat(dto.count()).isEqualTo(7L);
    }

    @Test
    void ratingEntryDto_construct_exposesFieldsViaAccessors() {
        AnalyticsResponseDto.RatingEntryDto dto = new AnalyticsResponseDto.RatingEntryDto(5, 9L);

        assertThat(dto.rating()).isEqualTo(5);
        assertThat(dto.count()).isEqualTo(9L);
    }

    @Test
    void analyticsResponseDto_construct_exposesNestedListsViaAccessors() {
        AnalyticsResponseDto dto = new AnalyticsResponseDto(
                List.of(new AnalyticsResponseDto.FolderStorageEntryDto("/photos", 1024L)),
                List.of(new AnalyticsResponseDto.FormatEntryDto("jpg", 5L)),
                List.of(new AnalyticsResponseDto.MonthlyCountEntryDto("2026-01", 7L)),
                List.of(new AnalyticsResponseDto.RatingEntryDto(5, 9L)));

        assertThat(dto.folderStorage()).hasSize(1);
        assertThat(dto.formatDistribution()).hasSize(1);
        assertThat(dto.photosPerMonth()).hasSize(1);
        assertThat(dto.ratingDistribution()).hasSize(1);
    }
}
