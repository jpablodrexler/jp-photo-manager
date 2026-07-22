package com.jpablodrexler.photomanager.infrastructure.web.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.exception.AssetNotFoundException;
import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.controller.HomeController;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.HomeWebMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the {@code global-error-handler} change: every 4xx/5xx response body is a consistent
 * {@code { status, message, timestamp }} JSON shape (here {@link com.jpablodrexler.photomanager.infrastructure.web.dto.response.ErrorResponseDto},
 * which is a superset carrying an additional {@code error} reason-phrase field).
 *
 * <p>The proposal's spec references generic exception type names ({@code ResourceNotFoundException},
 * {@code ValidationException}) that do not exist verbatim in this codebase; the concrete domain
 * exceptions below ({@link AlbumNotFoundException}, {@link AssetNotFoundException} for "not found",
 * {@link IllegalArgumentException} for "validation") stand in for them, exercising the same
 * scenarios described in {@code specs/global-error-handler/spec.md}.
 */
@WebMvcTest(HomeController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetHomeStatsUseCase getHomeStatsUseCase;
    @MockitoBean
    HomeWebMapper homeWebMapper;

    @Test
    void albumNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new AlbumNotFoundException(42L));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Album not found: 42"));
    }

    @Test
    void searchPresetNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new SearchPresetNotFoundException(7L));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Search preset not found: 7"));
    }

    @Test
    void resourceNotFoundException_returns404WithErrorBodyContainingExceptionMessage() throws Exception {
        // Stand-in for the spec's generic `ResourceNotFoundException` — this codebase models
        // "not found" as per-resource exceptions (AssetNotFoundException, FolderNotFoundException, ...).
        when(getHomeStatsUseCase.execute()).thenThrow(new AssetNotFoundException(99L));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Asset not found: 99"));
    }

    @Test
    void entityNotFoundException_returns404WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new EntityNotFoundException("Asset not found"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void illegalArgumentException_returns400WithErrorBody() throws Exception {
        // Stand-in for the spec's generic `ValidationException` — this codebase raises
        // IllegalArgumentException / MethodArgumentNotValidException for invalid input.
        when(getHomeStatsUseCase.execute()).thenThrow(new IllegalArgumentException("Bad input"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad input"));
    }

    @Test
    void authenticationException_returns401WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void invalidRefreshTokenException_returns401WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new InvalidRefreshTokenException("Token expired or not found"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token expired or not found"));
    }

    @Test
    void accessDeniedException_returns403WithErrorBody() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new AccessDeniedException("Cannot view another user's data"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void genericException_returns500WithGenericMessageNotExceptionDetail() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new RuntimeException("Some internal stack-trace-worthy detail"));

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void anyErrorResponse_timestampIsNonNullAndRecent() throws Exception {
        when(getHomeStatsUseCase.execute()).thenThrow(new AlbumNotFoundException(1L));

        String body = mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = new ObjectMapper().readTree(body);
        String timestamp = json.get("timestamp").asText();
        assertThat(timestamp).isNotBlank();

        Instant parsed = Instant.parse(timestamp);
        assertThat(Duration.between(parsed, Instant.now()).abs()).isLessThan(Duration.ofSeconds(10));
    }
}
