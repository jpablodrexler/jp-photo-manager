package com.jpablodrexler.photomanager.infrastructure.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KafkaProgressRegistry {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> completions = new ConcurrentHashMap<>();

    public void registerEmitter(long runId, SseEmitter emitter) {
        emitters.put(runId, emitter);
    }

    public void registerCompletion(long runId, CompletableFuture<Void> completion) {
        completions.put(runId, completion);
    }

    public SseEmitter getEmitter(long runId) {
        return emitters.get(runId);
    }

    public CompletableFuture<Void> getCompletion(long runId) {
        return completions.get(runId);
    }

    public void complete(long runId) {
        CompletableFuture<Void> completion = completions.get(runId);
        if (completion != null) {
            completion.complete(null);
        }
    }

    public void remove(long runId) {
        emitters.remove(runId);
        completions.remove(runId);
    }
}
