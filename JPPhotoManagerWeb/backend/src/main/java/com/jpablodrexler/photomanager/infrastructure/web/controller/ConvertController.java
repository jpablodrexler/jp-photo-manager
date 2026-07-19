package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.convert.ConvertAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.GetConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.SaveConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.ConvertDirectoryPairDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.ConvertWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Tag(name = "Convert", description = "PNG to JPEG conversion configuration and execution")
@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
@Slf4j
public class ConvertController {

    private final GetConvertConfigUseCase getConvertConfigUseCase;
    private final SaveConvertConfigUseCase saveConvertConfigUseCase;
    private final ConvertAssetsUseCase convertAssetsUseCase;
    private final KafkaProgressRegistry kafkaProgressRegistry;
    private final ConvertWebMapper convertWebMapper;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @Operation(summary = "Get convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convert configuration"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/configuration")
    public ResponseEntity<List<ConvertDirectoryPairDto>> getConfiguration() {
        return ResponseEntity.ok(convertWebMapper.toDtoList(getConvertConfigUseCase.execute()));
    }

    @Operation(summary = "Save convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Configuration saved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(@Valid @RequestBody List<ConvertDirectoryPairDto> definitions) {
        saveConvertConfigUseCase.execute(convertWebMapper.toDomainList(definitions));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Run PNG→JPEG conversion and stream progress via SSE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SSE stream of convert status and results"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        long runId = System.currentTimeMillis();
        Runnable cleanup = () -> kafkaProgressRegistry.remove(runId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());
        kafkaProgressRegistry.registerEmitter(runId, emitter);
        convertAssetsUseCase.execute(runId, resolveUserId());
        return emitter;
    }

    /**
     * Resolves the authenticated user's id for audit-logging purposes only. Never throws: returns
     * {@code null} when unauthenticated or the user cannot be resolved, so that an inability to
     * identify the caller never blocks the primary request.
     */
    private UUID resolveUserId() {
        try {
            return getCurrentUserUseCase.execute().getId();
        } catch (Exception e) {
            log.warn("Failed to resolve current user id for audit logging: {}", e.getMessage());
            return null;
        }
    }
}
