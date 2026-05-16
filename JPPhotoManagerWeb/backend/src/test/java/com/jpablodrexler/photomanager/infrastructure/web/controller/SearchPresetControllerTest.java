package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.search.CreateSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.DeleteSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.GetSearchPresetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.web.dto.CreatePresetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchPresetController.class)
@ActiveProfiles("test")
class SearchPresetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GetSearchPresetsUseCase getSearchPresetsUseCase;
    @MockitoBean
    CreateSearchPresetUseCase createSearchPresetUseCase;
    @MockitoBean
    DeleteSearchPresetUseCase deleteSearchPresetUseCase;
    @MockitoBean
    UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
    }

    // --- GET /api/search-presets ---

    @Test
    @WithMockUser("user")
    void list_returnsPresetsForCurrentUser() throws Exception {
        String filterJson = "{\"search\":\"beach\",\"dateFrom\":null,\"dateTo\":null,\"minRating\":null}";
        SearchPreset preset = SearchPreset.builder()
                .presetId(1L)
                .userId(userId)
                .name("Beach Photos")
                .filterJson(filterJson)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();

        when(getSearchPresetsUseCase.execute(userId)).thenReturn(List.of(preset));

        mockMvc.perform(get("/api/search-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presetId").value(1))
                .andExpect(jsonPath("$[0].name").value("Beach Photos"))
                .andExpect(jsonPath("$[0].search").value("beach"));
    }

    @Test
    @WithMockUser("user")
    void list_emptyPresets_returns200WithEmptyArray() throws Exception {
        when(getSearchPresetsUseCase.execute(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/search-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser("user")
    void list_presetWithInvalidFilterJson_returnsNullFilterFields() throws Exception {
        SearchPreset preset = SearchPreset.builder()
                .presetId(2L)
                .userId(userId)
                .name("Broken")
                .filterJson("not-json")
                .createdAt(Instant.now())
                .build();

        when(getSearchPresetsUseCase.execute(userId)).thenReturn(List.of(preset));

        mockMvc.perform(get("/api/search-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presetId").value(2))
                .andExpect(jsonPath("$[0].search").doesNotExist());
    }

    // --- POST /api/search-presets ---

    @Test
    @WithMockUser("user")
    void create_validRequest_returns201WithPreset() throws Exception {
        String filterJson = "{\"search\":\"sunset\",\"dateFrom\":null,\"dateTo\":null,\"minRating\":4}";
        SearchPreset created = SearchPreset.builder()
                .presetId(3L)
                .userId(userId)
                .name("Sunsets")
                .filterJson(filterJson)
                .createdAt(Instant.now())
                .build();

        when(createSearchPresetUseCase.execute(eq(userId), eq("Sunsets"), any(FilterPreset.class)))
                .thenReturn(created);

        CreatePresetRequest request = new CreatePresetRequest("Sunsets", "sunset", null, null, 4);
        mockMvc.perform(post("/api/search-presets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presetId").value(3))
                .andExpect(jsonPath("$.name").value("Sunsets"));
    }

    // --- DELETE /api/search-presets/{id} ---

    @Test
    @WithMockUser("user")
    void delete_existingPreset_returns204() throws Exception {
        doNothing().when(deleteSearchPresetUseCase).execute(1L, userId);

        mockMvc.perform(delete("/api/search-presets/1"))
                .andExpect(status().isNoContent());

        verify(deleteSearchPresetUseCase).execute(1L, userId);
    }
}
