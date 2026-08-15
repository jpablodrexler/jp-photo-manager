package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumWebMapperImplTest {

    private final AlbumWebMapper sut = new AlbumWebMapperImpl(new AlbumFilterJsonConverter(new ObjectMapper()));

    @Test
    void toSummaryDto_nullData_returnsNull() {
        assertThat(sut.toSummaryDto(null)).isNull();
    }

    @Test
    void toSummaryDto_dataWithFilterJson_mapsFieldsAndDeserializesFilterJson() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        AlbumData data = new AlbumData(1L, "Trip", "desc", createdAt, 5L,
                "{\"search\":\"sunset\",\"minRating\":4}");

        AlbumSummaryResponseDto result = sut.toSummaryDto(data);

        assertThat(result.getAlbumId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Trip");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getAssetCount()).isEqualTo(5L);
        assertThat(result.getFilterJson()).isNotNull();
        assertThat(result.getFilterJson().search()).isEqualTo("sunset");
        assertThat(result.getFilterJson().minRating()).isEqualTo(4);
    }

    @Test
    void toSummaryDto_dataWithNullFilterJson_leavesFilterJsonNull() {
        AlbumData data = new AlbumData(2L, "Plain", null, Instant.now(), 0L, null);

        AlbumSummaryResponseDto result = sut.toSummaryDto(data);

        assertThat(result.getFilterJson()).isNull();
    }

    @Test
    void toDto_nullSummaryAndAssets_returnsNull() {
        assertThat(sut.toDto(null, null)).isNull();
    }

    @Test
    void toDto_summaryAndAssetsProvided_mapsAllFields() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        AlbumData summary = new AlbumData(1L, "Trip", "desc", createdAt, 2L, "{\"search\":\"sunset\"}");
        AssetResponseDto assetDto = new AssetResponseDto();
        assetDto.setAssetId(10L);
        PaginatedData<AssetResponseDto> assets = new PaginatedData<>(List.of(assetDto), 0, 1, 2L);

        AlbumResponseDto result = sut.toDto(summary, assets);

        assertThat(result.getAlbumId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Trip");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getFilterJson()).isNotNull();
        assertThat(result.getFilterJson().search()).isEqualTo("sunset");
        assertThat(result.getAssets()).isSameAs(assets);
    }

    @Test
    void toDto_nullSummaryWithAssets_leavesSummaryFieldsNullButSetsAssets() {
        PaginatedData<AssetResponseDto> assets = new PaginatedData<>(List.of(), 0, 0, 0L);

        AlbumResponseDto result = sut.toDto(null, assets);

        assertThat(result.getAlbumId()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getAssets()).isSameAs(assets);
    }
}
