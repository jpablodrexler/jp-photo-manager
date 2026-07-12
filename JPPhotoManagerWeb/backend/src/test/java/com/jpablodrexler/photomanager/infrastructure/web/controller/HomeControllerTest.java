package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.AssetSummaryDto;
import com.jpablodrexler.photomanager.domain.model.FolderStat;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetHomeStatsUseCase getHomeStatsUseCase;

    // --- GET /api/home/stats ---

    @Test
    void getStats_returnsCountsAndLastCompleted() throws Exception {
        Instant lastCatalog = Instant.parse("2025-01-15T10:00:00Z");
        HomeStats stats = new HomeStats(42L, 1000L, lastCatalog, 0L, 0L, List.of(), List.of());
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(42))
                .andExpect(jsonPath("$.assetCount").value(1000))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").isNotEmpty());
    }

    @Test
    void getStats_noCompletedCatalog_returnsNullTimestamp() throws Exception {
        HomeStats stats = new HomeStats(0L, 0L, null, 0L, 0L, List.of(), List.of());
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(0))
                .andExpect(jsonPath("$.assetCount").value(0))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").doesNotExist());
    }

    @Test
    void getStats_returnsTotalFileSizeAndDuplicateCount() throws Exception {
        HomeStats stats = new HomeStats(5L, 200L, null, 26_112_000_000L, 3L, List.of(), List.of());
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileSize").value(26_112_000_000L))
                .andExpect(jsonPath("$.duplicateCount").value(3));
    }

    @Test
    void getStats_returnsTopFoldersList() throws Exception {
        List<FolderStat> topFolders = List.of(
                new FolderStat("/photos/vacation", 500L),
                new FolderStat("/photos/family", 200L));
        HomeStats stats = new HomeStats(2L, 700L, null, 0L, 0L, topFolders, List.of());
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topFolders").isArray())
                .andExpect(jsonPath("$.topFolders[0].path").value("/photos/vacation"))
                .andExpect(jsonPath("$.topFolders[0].assetCount").value(500))
                .andExpect(jsonPath("$.topFolders[1].path").value("/photos/family"));
    }

    @Test
    void getStats_returnsRecentAssetsList() throws Exception {
        List<AssetSummaryDto> recent = List.of(
                new AssetSummaryDto(1L, "sunset.jpg", "/photos/vacation", "/api/assets/1/thumbnail", 1024L),
                new AssetSummaryDto(2L, "beach.jpg", "/photos/summer", "/api/assets/2/thumbnail", 2048L));
        HomeStats stats = new HomeStats(1L, 2L, null, 0L, 0L, List.of(), recent);
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentAssets").isArray())
                .andExpect(jsonPath("$.recentAssets[0].assetId").value(1))
                .andExpect(jsonPath("$.recentAssets[0].fileName").value("sunset.jpg"))
                .andExpect(jsonPath("$.recentAssets[0].folderPath").value("/photos/vacation"))
                .andExpect(jsonPath("$.recentAssets[0].thumbnailUrl").value("/api/assets/1/thumbnail"))
                .andExpect(jsonPath("$.recentAssets[0].fileSize").value(1024))
                .andExpect(jsonPath("$.recentAssets[1].fileSize").value(2048));
    }
}
