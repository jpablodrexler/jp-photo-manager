package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
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
    PhotoManagerFacade facade;

    @Test
    void getStats_returnsCountsAndLastCompleted() throws Exception {
        Instant lastCompleted = Instant.parse("2026-05-01T10:00:00Z");
        when(facade.getHomeStats()).thenReturn(new HomeStats(42L, 1234L, lastCompleted));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(42))
                .andExpect(jsonPath("$.assetCount").value(1234))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").isNotEmpty());
    }

    @Test
    void getStats_noCompletedCatalog_returnsNullTimestamp() throws Exception {
        when(facade.getHomeStats()).thenReturn(new HomeStats(0L, 0L, null));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderCount").value(0))
                .andExpect(jsonPath("$.assetCount").value(0))
                .andExpect(jsonPath("$.lastCatalogCompletedAt").doesNotExist());
    }
}
