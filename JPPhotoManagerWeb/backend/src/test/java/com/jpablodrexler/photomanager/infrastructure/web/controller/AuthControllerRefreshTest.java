package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LogoutUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.AuthCookieFactory;
import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthCookieFactory.class)
@ActiveProfiles("test")
class AuthControllerRefreshTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoginUseCase loginUseCase;
    @MockitoBean
    RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean
    LogoutUseCase logoutUseCase;

    // --- POST /api/auth/refresh ---

    @Test
    void refresh_validToken_returns200WithNewJwtCookie() throws Exception {
        Instant newExpiry = Instant.parse("2025-12-31T00:00:00Z");
        RefreshTokenUseCase.RefreshResult result =
                new RefreshTokenUseCase.RefreshResult("alice", "new-jwt", newExpiry, "new-refresh");

        when(refreshTokenUseCase.execute("valid-refresh")).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "valid-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(refreshTokenUseCase.execute("bad-token"))
                .thenThrow(new InvalidRefreshTokenException("Token expired or not found"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "bad-token")))
                .andExpect(status().isUnauthorized());
    }
}
