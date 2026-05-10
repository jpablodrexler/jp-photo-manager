package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.dto.CreatePresetRequest;
import com.jpablodrexler.photomanager.api.dto.SearchPresetDto;
import com.jpablodrexler.photomanager.api.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
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
    PhotoManagerFacade facade;

    @MockitoBean
    UserRepository userRepository;

    private UUID userId;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        now = Instant.now();
        User user = new User();
        user.setId(userId);
        user.setUsername("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
    }

    @Test
    @WithMockUser("user")
    void list_authenticated_returns200WithPresetList() throws Exception {
        SearchPresetDto dto = new SearchPresetDto(1L, "Vacation 3-star", now, "vacation", null, null, 3);
        when(facade.listSearchPresets(userId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/search-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presetId").value(1))
                .andExpect(jsonPath("$[0].name").value("Vacation 3-star"))
                .andExpect(jsonPath("$[0].search").value("vacation"))
                .andExpect(jsonPath("$[0].minRating").value(3));
    }

    @Test
    @WithMockUser("user")
    void create_validRequest_returns201WithDto() throws Exception {
        SearchPresetDto created = new SearchPresetDto(2L, "Test", now, null, null, null, null);
        when(facade.saveSearchPreset(eq(userId), any())).thenReturn(created);

        CreatePresetRequest req = new CreatePresetRequest("Test", null, null, null, null);
        mockMvc.perform(post("/api/search-presets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presetId").value(2))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    @WithMockUser("user")
    void create_emptyName_returns400() throws Exception {
        CreatePresetRequest req = new CreatePresetRequest("", null, null, null, null);
        mockMvc.perform(post("/api/search-presets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("user")
    void delete_existingPreset_returns204() throws Exception {
        doNothing().when(facade).deleteSearchPreset(userId, 1L);

        mockMvc.perform(delete("/api/search-presets/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser("user")
    void delete_unknownPreset_returns404() throws Exception {
        doThrow(new SearchPresetNotFoundException(999L)).when(facade).deleteSearchPreset(userId, 999L);

        mockMvc.perform(delete("/api/search-presets/999"))
                .andExpect(status().isNotFound());
    }
}
