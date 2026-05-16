package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.GetSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SaveSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final GetSyncConfigUseCase getSyncConfigUseCase;
    private final SaveSyncConfigUseCase saveSyncConfigUseCase;
    private final SyncAssetsUseCase syncAssetsUseCase;

    @GetMapping("/configuration")
    public ResponseEntity<List<SyncDirectoriesDefinition>> getConfiguration() {
        return ResponseEntity.ok(getSyncConfigUseCase.execute());
    }

    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(@Valid @RequestBody List<SyncDirectoriesDefinition> definitions) {
        saveSyncConfigUseCase.execute(definitions);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                List<SyncAssetsResult> results = syncAssetsUseCase.execute(status -> {
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
