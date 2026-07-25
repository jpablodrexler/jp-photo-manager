package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.auth.GetActiveSessionsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.LogoutUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.domain.port.in.auth.RevokeSessionUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.AuthCookieFactory;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AuthRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.LoginResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.MeResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SessionResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.SessionWebMapper;
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
import java.util.List;
import java.util.Optional;

@Tag(name = "Authentication", description = "Login, logout, and token refresh")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final AuthCookieFactory authCookieFactory;
    private final SessionWebMapper sessionWebMapper;

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
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse response) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        LoginUseCase.LoginResult result = loginUseCase.execute(request.username(), request.password(), userAgent);

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

    @Operation(summary = "List the authenticated user's active sessions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active sessions"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponseDto>> sessions(HttpServletRequest request) {
        String currentRefreshTokenValue = extractCookieValue(request, "refreshToken").orElse(null);
        return ResponseEntity.ok(sessionWebMapper.toDtoList(getActiveSessionsUseCase.execute(currentRefreshTokenValue)));
    }

    @Operation(summary = "Revoke a single session")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session revoked"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@PathVariable long id) {
        revokeSessionUseCase.revokeOne(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke all sessions except the current one (sign out everywhere else)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Other sessions revoked"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeOtherSessions(HttpServletRequest request) {
        String currentRefreshTokenValue = extractCookieValue(request, "refreshToken").orElse(null);
        revokeSessionUseCase.revokeAllOthers(currentRefreshTokenValue);
        return ResponseEntity.noContent().build();
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
