package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

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
        HomeStats stats = new HomeStats(42L, 1000L, lastCatalog);
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(42))
                .andExpect(jsonPath("$.assetCount").value(1000))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").isNotEmpty());
    }

    @Test
    void getStats_noCompletedCatalog_returnsNullTimestamp() throws Exception {
        HomeStats stats = new HomeStats(0L, 0L, null);
        when(getHomeStatsUseCase.execute()).thenReturn(stats);

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(0))
                .andExpect(jsonPath("$.assetCount").value(0))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").doesNotExist());
    }
}
