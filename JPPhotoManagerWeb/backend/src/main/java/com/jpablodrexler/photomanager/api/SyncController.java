package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
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

    private final PhotoManagerFacade facade;

    @GetMapping("/configuration")
    public ResponseEntity<List<SyncAssetsDirectoriesDefinition>> getConfiguration() {
        return ResponseEntity.ok(facade.getSyncAssetsConfiguration());
    }

    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(
            @Valid @RequestBody List<SyncAssetsDirectoriesDefinition> definitions) {
        facade.setSyncAssetsConfiguration(definitions);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                List<SyncAssetsResult> results = facade.syncAssetsAsync(status -> {
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
