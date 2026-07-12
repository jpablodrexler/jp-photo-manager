package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;
import com.jpablodrexler.photomanager.domain.port.in.analytics.GetAnalyticsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AnalyticsResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AnalyticsMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetAnalyticsUseCase getAnalyticsUseCase;

    @MockitoBean
    AnalyticsMapper analyticsMapper;

    @Test
    void getAnalytics_authenticated_returns200WithBody() throws Exception {
        AnalyticsData data = AnalyticsData.builder()
                .folderStorage(List.of())
                .formatDistribution(List.of())
                .photosPerMonth(List.of())
                .ratingDistribution(List.of())
                .build();
        AnalyticsResponseDto dto = new AnalyticsResponseDto(List.of(), List.of(), List.of(), List.of());
        when(getAnalyticsUseCase.execute()).thenReturn(data);
        when(analyticsMapper.toDto(data)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderStorage").isArray())
                .andExpect(jsonPath("$.formatDistribution").isArray())
                .andExpect(jsonPath("$.photosPerMonth").isArray())
                .andExpect(jsonPath("$.ratingDistribution").isArray());
    }

    @Test
    void getAnalytics_emptyData_returns200WithEmptyArrays() throws Exception {
        AnalyticsData empty = AnalyticsData.builder()
                .folderStorage(List.of())
                .formatDistribution(List.of())
                .photosPerMonth(List.of())
                .ratingDistribution(List.of())
                .build();
        AnalyticsResponseDto emptyDto = new AnalyticsResponseDto(List.of(), List.of(), List.of(), List.of());
        when(getAnalyticsUseCase.execute()).thenReturn(empty);
        when(analyticsMapper.toDto(empty)).thenReturn(emptyDto);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderStorage").isEmpty())
                .andExpect(jsonPath("$.formatDistribution").isEmpty());
    }
}
