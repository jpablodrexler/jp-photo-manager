package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenService;
import com.jpablodrexler.photomanager.domain.port.out.UserService;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AuthRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;
    @MockitoBean
    JwtTokenPort jwtTokenService;
    @MockitoBean
    RefreshTokenService refreshTokenService;

    // --- POST /api/auth/login ---

    @Test
    void login_validCredentials_returns200WithUsernameAndSetsJwtCookie() throws Exception {
        Instant expiry = Instant.parse("2025-12-31T00:00:00Z");
        when(userService.authenticate("admin", "admin")).thenReturn("jwt-token");
        when(jwtTokenService.tokenExpiry("jwt-token")).thenReturn(expiry);
        when(refreshTokenService.issueRefreshToken("admin")).thenReturn("refresh-token");

        AuthRequestDto request = new AuthRequestDto("admin", "admin");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(userService.authenticate("admin", "wrong"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthRequestDto request = new AuthRequestDto("admin", "wrong");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        AuthRequestDto request = new AuthRequestDto("", "pass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/auth/logout ---

    @Test
    void logout_clearsCookiesAndReturns200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }
}
