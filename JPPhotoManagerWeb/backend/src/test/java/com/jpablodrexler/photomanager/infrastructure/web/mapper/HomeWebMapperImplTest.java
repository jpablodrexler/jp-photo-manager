package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetSummary;
import com.jpablodrexler.photomanager.domain.model.FolderStat;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.FolderStatResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.HomeStatsResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomeWebMapperImplTest {

    private final HomeWebMapper sut = new HomeWebMapperImpl();

    @Test
    void toDto_homeStats_mapsScalarsAndNestedLists() {
        Instant lastCataloged = Instant.parse("2026-01-01T00:00:00Z");
        HomeStats stats = new HomeStats(
                10L,
                200L,
                lastCataloged,
                123456L,
                3L,
                List.of(new FolderStat("/photos", 50L)),
                List.of(new AssetSummary(1L, "a.jpg", "/photos", "/api/assets/1/thumbnail", 2048L)));

        HomeStatsResponseDto result = sut.toDto(stats);

        assertThat(result.folderCount()).isEqualTo(10L);
        assertThat(result.assetCount()).isEqualTo(200L);
        assertThat(result.lastCatalogCompletedAt()).isEqualTo(lastCataloged);
        assertThat(result.totalFileSize()).isEqualTo(123456L);
        assertThat(result.duplicateCount()).isEqualTo(3L);
        assertThat(result.topFolders()).hasSize(1);
        assertThat(result.topFolders().get(0).path()).isEqualTo("/photos");
        assertThat(result.topFolders().get(0).assetCount()).isEqualTo(50L);
        assertThat(result.recentAssets()).hasSize(1);
        assertThat(result.recentAssets().get(0).assetId()).isEqualTo(1L);
        assertThat(result.recentAssets().get(0).fileName()).isEqualTo("a.jpg");
    }

    @Test
    void toDto_nullHomeStats_returnsNull() {
        assertThat(sut.toDto((HomeStats) null)).isNull();
    }

    @Test
    void toDto_assetSummary_mapsFields() {
        AssetSummary summary = new AssetSummary(2L, "b.jpg", "/videos", "/api/assets/2/thumbnail", 4096L);

        AssetSummaryResponseDto result = sut.toDto(summary);

        assertThat(result.assetId()).isEqualTo(2L);
        assertThat(result.fileName()).isEqualTo("b.jpg");
        assertThat(result.folderPath()).isEqualTo("/videos");
        assertThat(result.thumbnailUrl()).isEqualTo("/api/assets/2/thumbnail");
        assertThat(result.fileSize()).isEqualTo(4096L);
    }

    @Test
    void toDto_folderStat_mapsFields() {
        FolderStat stat = new FolderStat("/archive", 99L);

        FolderStatResponseDto result = sut.toDto(stat);

        assertThat(result.path()).isEqualTo("/archive");
        assertThat(result.assetCount()).isEqualTo(99L);
    }
}
