package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.GetSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SaveSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Sync", description = "Directory synchronisation configuration and execution")
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final GetSyncConfigUseCase getSyncConfigUseCase;
    private final SaveSyncConfigUseCase saveSyncConfigUseCase;
    private final SyncAssetsUseCase syncAssetsUseCase;
    private final KafkaProgressRegistry kafkaProgressRegistry;

    @Operation(summary = "Get sync directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sync configuration"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/configuration")
    public ResponseEntity<List<SyncDirectoriesDefinition>> getConfiguration() {
        return ResponseEntity.ok(getSyncConfigUseCase.execute());
    }

    @Operation(summary = "Save sync directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Configuration saved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(@Valid @RequestBody List<SyncDirectoriesDefinition> definitions) {
        saveSyncConfigUseCase.execute(definitions);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Run sync and stream progress via SSE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SSE stream of sync status and results"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        long runId = System.currentTimeMillis();
        kafkaProgressRegistry.registerEmitter(runId, emitter);
        syncAssetsUseCase.execute(runId);
        return emitter;
    }
}
