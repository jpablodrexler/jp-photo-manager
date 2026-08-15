package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.model.MonthlyCountEntry;
import com.jpablodrexler.photomanager.domain.model.RatingEntry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AnalyticsResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsWebMapperImplTest {

    private final AnalyticsWebMapper sut = new AnalyticsWebMapperImpl();

    @Test
    void toDto_analyticsData_mapsAllNestedLists() {
        AnalyticsData data = AnalyticsData.builder()
                .folderStorage(List.of(new FolderStorageEntry("/photos", 1024L)))
                .formatDistribution(List.of(new FormatEntry("jpg", 42L)))
                .photosPerMonth(List.of(new MonthlyCountEntry("2026-01", 10L)))
                .ratingDistribution(List.of(new RatingEntry(5, 3L)))
                .build();

        AnalyticsResponseDto result = sut.toDto(data);

        assertThat(result.folderStorage()).hasSize(1);
        assertThat(result.folderStorage().get(0).folderPath()).isEqualTo("/photos");
        assertThat(result.folderStorage().get(0).bytes()).isEqualTo(1024L);
        assertThat(result.formatDistribution()).hasSize(1);
        assertThat(result.formatDistribution().get(0).extension()).isEqualTo("jpg");
        assertThat(result.formatDistribution().get(0).count()).isEqualTo(42L);
        assertThat(result.photosPerMonth()).hasSize(1);
        assertThat(result.photosPerMonth().get(0).month()).isEqualTo("2026-01");
        assertThat(result.photosPerMonth().get(0).count()).isEqualTo(10L);
        assertThat(result.ratingDistribution()).hasSize(1);
        assertThat(result.ratingDistribution().get(0).rating()).isEqualTo(5);
        assertThat(result.ratingDistribution().get(0).count()).isEqualTo(3L);
    }

    @Test
    void toDto_nullAnalyticsData_returnsNull() {
        assertThat(sut.toDto((AnalyticsData) null)).isNull();
    }

    @Test
    void toDto_folderStorageEntry_mapsFields() {
        AnalyticsResponseDto.FolderStorageEntryDto result = sut.toDto(new FolderStorageEntry("/videos", 2048L));

        assertThat(result.folderPath()).isEqualTo("/videos");
        assertThat(result.bytes()).isEqualTo(2048L);
    }

    @Test
    void toDto_formatEntry_mapsFields() {
        AnalyticsResponseDto.FormatEntryDto result = sut.toDto(new FormatEntry("png", 7L));

        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.count()).isEqualTo(7L);
    }

    @Test
    void toDto_monthlyCountEntry_mapsFields() {
        AnalyticsResponseDto.MonthlyCountEntryDto result = sut.toDto(new MonthlyCountEntry("2026-02", 20L));

        assertThat(result.month()).isEqualTo("2026-02");
        assertThat(result.count()).isEqualTo(20L);
    }

    @Test
    void toDto_ratingEntry_mapsFields() {
        AnalyticsResponseDto.RatingEntryDto result = sut.toDto(new RatingEntry(4, 15L));

        assertThat(result.rating()).isEqualTo(4);
        assertThat(result.count()).isEqualTo(15L);
    }
}
