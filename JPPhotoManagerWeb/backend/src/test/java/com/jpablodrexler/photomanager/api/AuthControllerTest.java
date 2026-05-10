package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.service.JwtTokenService;
import com.jpablodrexler.photomanager.domain.service.RefreshTokenService;
import com.jpablodrexler.photomanager.domain.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
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
class AuthControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @MockitoBean
        UserService userService;

        @MockitoBean
        JwtTokenService jwtTokenService;

        @MockitoBean
        RefreshTokenService refreshTokenService;

        @Test
        void login_validCredentials_setsBothCookiesAndReturnsLoginResponse() throws Exception {
                Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
                when(userService.authenticate("alice", "secret")).thenReturn("jwt-token");
                when(jwtTokenService.tokenExpiry("jwt-token")).thenReturn(expiresAt);
                when(refreshTokenService.issueRefreshToken("alice")).thenReturn("refresh-token-value");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"alice","password":"secret"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(containsString("jwt=jwt-token"))))
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(containsString("refreshToken=refresh-token-value"))))
                                .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        void login_invalidCredentials_returns401() throws Exception {
                when(userService.authenticate(anyString(), anyString()))
                                .thenThrow(new BadCredentialsException("Invalid credentials"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"alice","password":"wrong"}
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_clearsBothCookies() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isOk())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(containsString("jwt="))))
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(containsString("Max-Age=0"))));
        }
}
