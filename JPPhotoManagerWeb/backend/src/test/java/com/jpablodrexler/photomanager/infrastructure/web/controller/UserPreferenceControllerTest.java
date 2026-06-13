package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.in.preference.GetUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.in.preference.SaveUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.web.dto.UserPreferenceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferenceController.class)
@ActiveProfiles("test")
class UserPreferenceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean GetUserPreferenceUseCase getPreference;
    @MockitoBean SaveUserPreferenceUseCase savePreference;
    @MockitoBean UserRepository userRepository;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("admin").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
    }

    @Test
    @WithMockUser("user")
    void getPreference_authenticated_returns200WithThemeMode() throws Exception {
        when(getPreference.execute(userId)).thenReturn(new UserPreference(userId, "light"));

        mockMvc.perform(get("/api/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("light"));
    }

    @Test
    @WithMockUser("user")
    void savePreference_authenticated_returns200() throws Exception {
        UserPreferenceDto dto = new UserPreferenceDto("dark");

        mockMvc.perform(put("/api/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(savePreference).execute(eq(userId), eq("dark"));
    }
}
