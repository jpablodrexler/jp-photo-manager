package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.application.dto.ConvertProgressMessage;
import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProgressListener {

    private final KafkaProgressRegistry registry;

    @KafkaListener(topics = "job.catalog.progress", groupId = "sse-broadcaster",
            containerFactory = "kafkaListenerContainerFactory")
    public void onCatalogProgress(CatalogProgressMessage message) {
        long runId = message.runId();
        SseEmitter emitter = registry.getEmitter(runId);

        if (message.done()) {
            if (emitter != null) {
                emitter.complete();
            }
            registry.complete(runId);
            registry.remove(runId);
            broadcastCatalogDone();
            return;
        }

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("catalog").data(message.notification()));
            } catch (IOException e) {
                log.warn("Failed to send catalog SSE event for runId={}: {}", runId, e.getMessage());
            }
        }

        broadcastCatalogProgress(message);
    }

    private void broadcastCatalogProgress(CatalogProgressMessage message) {
        for (SseEmitter observer : registry.getCatalogObservers()) {
            try {
                observer.send(SseEmitter.event().name("catalog").data(message.notification()));
            } catch (IOException e) {
                log.warn("Failed to broadcast catalog progress to observer: {}", e.getMessage());
                registry.removeCatalogObserver(observer);
            }
        }
    }

    private void broadcastCatalogDone() {
        for (SseEmitter observer : registry.getCatalogObservers()) {
            try {
                observer.send(SseEmitter.event().name("catalog-done").data("done"));
            } catch (IOException e) {
                log.warn("Failed to broadcast catalog-done to observer: {}", e.getMessage());
                registry.removeCatalogObserver(observer);
            }
        }
    }

    @KafkaListener(topics = "job.sync.progress", groupId = "sse-broadcaster",
            containerFactory = "kafkaListenerContainerFactory")
    public void onSyncProgress(SyncProgressMessage message) {
        long runId = message.runId();
        SseEmitter emitter = registry.getEmitter(runId);

        if (message.done()) {
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name("results").data(message.results()));
                } catch (IOException e) {
                    log.warn("Failed to send sync results SSE event for runId={}: {}", runId, e.getMessage());
                }
                emitter.complete();
            }
            registry.complete(runId);
            registry.remove(runId);
            return;
        }

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("status").data(message.status()));
        } catch (IOException e) {
            log.warn("Failed to send sync SSE event for runId={}: {}", runId, e.getMessage());
        }
    }

    @KafkaListener(topics = "job.convert.progress", groupId = "sse-broadcaster",
            containerFactory = "kafkaListenerContainerFactory")
    public void onConvertProgress(ConvertProgressMessage message) {
        long runId = message.runId();
        SseEmitter emitter = registry.getEmitter(runId);

        if (message.done()) {
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name("results").data(message.results()));
                } catch (IOException e) {
                    log.warn("Failed to send convert results SSE event for runId={}: {}", runId, e.getMessage());
                }
                emitter.complete();
            }
            registry.complete(runId);
            registry.remove(runId);
            return;
        }

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("status").data(message.status()));
        } catch (IOException e) {
            log.warn("Failed to send convert SSE event for runId={}: {}", runId, e.getMessage());
        }
    }
}
