package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.convert.ConvertAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.GetConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.SaveConvertConfigUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "Convert", description = "PNG to JPEG conversion configuration and execution")
@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ConvertController {

    private final GetConvertConfigUseCase getConvertConfigUseCase;
    private final SaveConvertConfigUseCase saveConvertConfigUseCase;
    private final ConvertAssetsUseCase convertAssetsUseCase;

    @Operation(summary = "Get convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convert configuration"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/configuration")
    public ResponseEntity<List<ConvertDirectoriesDefinition>> getConfiguration() {
        return ResponseEntity.ok(getConvertConfigUseCase.execute());
    }

    @Operation(summary = "Save convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Configuration saved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(@Valid @RequestBody List<ConvertDirectoriesDefinition> definitions) {
        saveConvertConfigUseCase.execute(definitions);
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                List<ConvertAssetsResult> results = convertAssetsUseCase.execute(status -> {
                    try {
                        emitter.send(SseEmitter.event().name("status").data(status));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }).get();
                emitter.send(SseEmitter.event().name("results").data(results));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }
}
