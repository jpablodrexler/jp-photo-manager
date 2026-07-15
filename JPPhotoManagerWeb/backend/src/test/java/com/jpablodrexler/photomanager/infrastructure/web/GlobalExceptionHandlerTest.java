package com.jpablodrexler.photomanager.infrastructure.web;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.controller.HomeController;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.HomeWebMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetHomeStatsUseCase getHomeStatsUseCase;
    @MockitoBean
    HomeWebMapper homeWebMapper;

    @Test
    void albumNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new AlbumNotFoundException(42L));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Album not found: 42"));
    }

    @Test
    void searchPresetNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new SearchPresetNotFoundException(7L));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Search preset not found: 7"));
    }

    @Test
    void entityNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new EntityNotFoundException("Asset not found"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void illegalArgumentException_returns400WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new IllegalArgumentException("Bad input"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad input"));
    }

    @Test
    void genericException_returns500WithGenericMessage() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An internal error occurred."));
    }
}
