package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProgressRegistryTest {

    KafkaProgressRegistry sut;

    @BeforeEach
    void setUp() {
        sut = new KafkaProgressRegistry();
    }

    @Test
    void registerEmitter_getEmitter_returnsRegisteredEmitter() {
        SseEmitter emitter = new SseEmitter();
        sut.registerEmitter(1L, emitter);

        assertThat(sut.getEmitter(1L)).isSameAs(emitter);
    }

    @Test
    void getEmitter_unknownRunId_returnsNull() {
        assertThat(sut.getEmitter(99L)).isNull();
    }

    @Test
    void registerCompletion_complete_completesTheFuture() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        sut.registerCompletion(1L, future);

        sut.complete(1L);

        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void complete_unknownRunId_doesNotThrow() {
        sut.complete(99L);
    }

    @Test
    void remove_clearsEmitterAndCompletion() {
        SseEmitter emitter = new SseEmitter();
        CompletableFuture<Void> future = new CompletableFuture<>();
        sut.registerEmitter(1L, emitter);
        sut.registerCompletion(1L, future);

        sut.remove(1L);

        assertThat(sut.getEmitter(1L)).isNull();
        assertThat(sut.getCompletion(1L)).isNull();
    }

    @Test
    void concurrentRegistration_doesNotLoseEntries() throws Exception {
        int count = 100;
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            long runId = i;
            threads[i] = new Thread(() -> sut.registerEmitter(runId, new SseEmitter()));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (int i = 0; i < count; i++) {
            assertThat(sut.getEmitter(i)).isNotNull();
        }
    }
}
