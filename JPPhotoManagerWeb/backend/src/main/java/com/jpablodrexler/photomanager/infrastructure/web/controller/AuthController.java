package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenService;
import com.jpablodrexler.photomanager.domain.port.out.UserService;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AuthRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.LoginResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.MeResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Tag(name = "Authentication", description = "Login, logout, and token refresh")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenPort jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Get the current authenticated user's username and role")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user info"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("VIEWER");
        return ResponseEntity.ok(new MeResponseDto(username, role));
    }

    @Operation(summary = "Authenticate and receive JWT cookie")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated; JWT set in HttpOnly cookie"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody AuthRequestDto request,
                                               HttpServletResponse response) {
        try {
            String token = userService.authenticate(request.username(), request.password());
            Instant expiresAt = jwtTokenService.tokenExpiry(token);

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(Duration.between(Instant.now(), expiresAt))
                    .build();

            String refreshTokenValue = refreshTokenService.issueRefreshToken(request.username());
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshTokenValue)
                    .httpOnly(true)
                    .path("/api/auth/refresh")
                    .sameSite("Strict")
                    .maxAge(Duration.ofDays(30))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            return ResponseEntity.ok(new LoginResponseDto(request.username().toLowerCase(), expiresAt));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Rotate refresh token and issue new JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New JWT issued; refresh token rotated"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = extractCookieValue(request, "refreshToken").orElse(null);
        if (tokenValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            RefreshTokenService.RotatedToken rotated = refreshTokenService.validateAndRotate(tokenValue);

            String newJwt = jwtTokenService.generateToken(rotated.username());
            Instant newExpiresAt = jwtTokenService.tokenExpiry(newJwt);

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", newJwt)
                    .httpOnly(true)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(Duration.between(Instant.now(), newExpiresAt))
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", rotated.newTokenValue())
                    .httpOnly(true)
                    .path("/api/auth/refresh")
                    .sameSite("Strict")
                    .maxAge(Duration.ofDays(30))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            return ResponseEntity.ok(new LoginResponseDto(rotated.username(), newExpiresAt));
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Logout and clear JWT cookie")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out; JWT cookie cleared")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String username = resolveUsernameForLogout(request);
        if (username != null) {
            try {
                refreshTokenService.revokeAllForUser(username);
            } catch (Exception e) {
                // best-effort revocation
            }
        }

        ResponseCookie jwtClear = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        ResponseCookie refreshClear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/api/auth/refresh")
                .sameSite("Strict")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtClear.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshClear.toString());
        return ResponseEntity.ok().build();
    }

    private String resolveUsernameForLogout(HttpServletRequest request) {
        Optional<String> jwtValue = extractCookieValue(request, "jwt");
        if (jwtValue.isPresent()) {
            try {
                return jwtTokenService.extractUsername(jwtValue.get());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
