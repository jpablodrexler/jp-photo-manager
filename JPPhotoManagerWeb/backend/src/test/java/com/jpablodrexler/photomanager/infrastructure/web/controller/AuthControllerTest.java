package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.exception.MissingRefreshTokenException;
import com.jpablodrexler.photomanager.application.exception.SessionNotFoundException;
import com.jpablodrexler.photomanager.domain.model.SessionInfo;
import com.jpablodrexler.photomanager.domain.port.in.auth.GetActiveSessionsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LogoutUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RevokeSessionUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.AuthCookieFactory;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AuthRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SessionResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.exception.GlobalExceptionHandler;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.SessionWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({ AuthCookieFactory.class, GlobalExceptionHandler.class })
@ActiveProfiles("test")
class AuthControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @MockitoBean
        LoginUseCase loginUseCase;
        @MockitoBean
        RefreshTokenUseCase refreshTokenUseCase;
        @MockitoBean
        LogoutUseCase logoutUseCase;
        @MockitoBean
        GetActiveSessionsUseCase getActiveSessionsUseCase;
        @MockitoBean
        RevokeSessionUseCase revokeSessionUseCase;
        @MockitoBean
        SessionWebMapper sessionWebMapper;

        // --- POST /api/auth/login ---

        @Test
        void login_validCredentials_returns200WithUsernameAndSetsJwtCookie() throws Exception {
                Instant expiry = Instant.parse("2025-12-31T00:00:00Z");
                when(loginUseCase.execute(eq("admin"), eq("admin"), any()))
                                .thenReturn(new LoginUseCase.LoginResult("admin", "jwt-token", expiry,
                                                "refresh-token"));

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
                when(loginUseCase.execute(eq("admin"), eq("wrong"), any()))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                AuthRequestDto request = new AuthRequestDto("admin", "wrong");
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message").value("Invalid username or password."));
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

        // --- GET /api/auth/sessions ---

        @Test
        void sessions_noRefreshCookie_passesNullCurrentTokenAndReturns200() throws Exception {
                Instant lastUsed = Instant.parse("2025-06-01T00:00:00Z");
                SessionInfo sessionInfo = new SessionInfo(1L, "Chrome on Windows", lastUsed, false);
                SessionResponseDto dto = new SessionResponseDto(1L, "Chrome on Windows", lastUsed, false);
                when(getActiveSessionsUseCase.execute(isNull())).thenReturn(List.of(sessionInfo));
                when(sessionWebMapper.toDtoList(List.of(sessionInfo))).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/auth/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].deviceHint").value("Chrome on Windows"))
                                .andExpect(jsonPath("$[0].current").value(false));
        }

        @Test
        void sessions_withRefreshCookie_passesCookieValueToUseCase() throws Exception {
                when(getActiveSessionsUseCase.execute("refresh-value")).thenReturn(List.of());
                when(sessionWebMapper.toDtoList(List.of())).thenReturn(List.of());

                mockMvc.perform(get("/api/auth/sessions")
                                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-value")))
                                .andExpect(status().isOk());

                verify(getActiveSessionsUseCase).execute("refresh-value");
        }

        // --- DELETE /api/auth/sessions/{id} ---

        @Test
        void revokeSession_ownSession_returns204() throws Exception {
                mockMvc.perform(delete("/api/auth/sessions/{id}", 5L))
                                .andExpect(status().isNoContent());

                verify(revokeSessionUseCase).revokeOne(5L);
        }

        @Test
        void revokeSession_notFound_returns404() throws Exception {
                doThrow(new SessionNotFoundException(99L)).when(revokeSessionUseCase).revokeOne(99L);

                mockMvc.perform(delete("/api/auth/sessions/{id}", 99L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        // --- DELETE /api/auth/sessions ---

        @Test
        void revokeOtherSessions_withRefreshCookie_passesCookieValueAndReturns204() throws Exception {
                mockMvc.perform(delete("/api/auth/sessions")
                                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-value")))
                                .andExpect(status().isNoContent());

                verify(revokeSessionUseCase).revokeAllOthers("refresh-value");
        }

        @Test
        void revokeOtherSessions_missingRefreshCookie_returns401() throws Exception {
                doThrow(new MissingRefreshTokenException()).when(revokeSessionUseCase).revokeAllOthers(null);

                mockMvc.perform(delete("/api/auth/sessions"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message").value("Refresh token cookie is missing"));
        }
}
