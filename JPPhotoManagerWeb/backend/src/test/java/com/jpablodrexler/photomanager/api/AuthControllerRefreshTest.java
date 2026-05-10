package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.exception.InvalidRefreshTokenException;
import com.jpablodrexler.photomanager.domain.service.JwtTokenService;
import com.jpablodrexler.photomanager.domain.service.RefreshTokenService;
import com.jpablodrexler.photomanager.domain.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerRefreshTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @Test
    void refresh_validRefreshTokenCookie_returns200AndSetsBothCookies() throws Exception {
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        RefreshTokenService.RotatedToken rotated =
                new RefreshTokenService.RotatedToken("new-rt", "alice", expiresAt);

        when(refreshTokenService.validateAndRotate("valid-rt")).thenReturn(rotated);
        when(jwtTokenService.generateToken("alice")).thenReturn("new-jwt");
        when(jwtTokenService.tokenExpiry("new-jwt")).thenReturn(expiresAt);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-rt")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(containsString("jwt=new-jwt"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(containsString("refreshToken=new-rt"))))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void refresh_noRefreshTokenCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_invalidRefreshToken_returns401() throws Exception {
        when(refreshTokenService.validateAndRotate(anyString()))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "bad-token")))
                .andExpect(status().isUnauthorized());
    }
}
