package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class SseNotificationRegistry {

    private final ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> completions = new ConcurrentHashMap<>();

    public void register(long runId, Consumer<CatalogChangeNotification> consumer, CompletableFuture<Void> completion) {
        if (consumer != null) {
            consumers.put(runId, consumer);
        }
        if (completion != null) {
            completions.put(runId, completion);
        }
    }

    public Consumer<CatalogChangeNotification> get(long runId) {
        return consumers.get(runId);
    }

    public void complete(long runId) {
        CompletableFuture<Void> future = completions.get(runId);
        if (future != null) {
            future.complete(null);
        }
    }

    public void remove(long runId) {
        consumers.remove(runId);
        completions.remove(runId);
    }
}
