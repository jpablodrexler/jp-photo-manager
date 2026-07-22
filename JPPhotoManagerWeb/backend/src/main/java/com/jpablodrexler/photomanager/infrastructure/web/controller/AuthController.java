package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LogoutUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.AuthCookieFactory;
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
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookieFactory authCookieFactory;

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
        LoginUseCase.LoginResult result = loginUseCase.execute(request.username(), request.password());

        ResponseCookie jwtCookie = authCookieFactory.jwtCookie(result.jwtToken(),
                Duration.between(Instant.now(), result.jwtExpiresAt()));
        ResponseCookie refreshCookie = authCookieFactory.refreshCookie(result.refreshTokenValue(),
                Duration.ofDays(30));

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(new LoginResponseDto(result.username(), result.jwtExpiresAt()));
    }

    @Operation(summary = "Rotate refresh token and issue new JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New JWT issued; refresh token rotated"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = extractCookieValue(request, "refreshToken")
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token cookie is missing"));

        RefreshTokenUseCase.RefreshResult result = refreshTokenUseCase.execute(tokenValue);

        ResponseCookie jwtCookie = authCookieFactory.jwtCookie(result.jwtToken(),
                Duration.between(Instant.now(), result.jwtExpiresAt()));
        ResponseCookie refreshCookie = authCookieFactory.refreshCookie(result.newRefreshTokenValue(),
                Duration.ofDays(30));

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(new LoginResponseDto(result.username(), result.jwtExpiresAt()));
    }

    @Operation(summary = "Logout and clear JWT cookie")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out; JWT cookie cleared")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String jwtValue = extractCookieValue(request, "jwt").orElse(null);
        logoutUseCase.execute(jwtValue);

        ResponseCookie jwtClear = authCookieFactory.clearJwtCookie();
        ResponseCookie refreshClear = authCookieFactory.clearRefreshCookie();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtClear.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshClear.toString());
        return ResponseEntity.ok().build();
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
