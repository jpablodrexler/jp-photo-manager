package com.jpablodrexler.photomanager.infrastructure.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring can invoke more than one of an {@link SseEmitter}'s onCompletion/onTimeout/onError
 * callbacks for the same connection (confirmed in production: the
 * {@code photomanager_active_sse_connections} gauge drifted to a negative count, only possible
 * if a decrement ran more than once per connection). For a plain counter that's just a wrong
 * metric, but for any cleanup that also unregisters the emitter from a
 * {@code KafkaProgressRegistry} entry, the same double-fire means a still-alive connection can be
 * unregistered before its real progress/done event ever arrives — confirmed as the root cause of
 * two separate-looking symptoms found via release-e2e-suite: the "Run catalog" spinner never
 * appearing, and an upload's status icon timing out even though the backend had actually
 * finished processing that file.
 */
public final class SseCleanup {

    private SseCleanup() {
    }

    /** Registers {@code cleanup} against every terminal callback, guaranteed to run at most once. */
    public static void registerOnce(SseEmitter emitter, Runnable cleanup) {
        AtomicBoolean alreadyCleanedUp = new AtomicBoolean(false);
        Runnable onceOnly = () -> {
            if (alreadyCleanedUp.compareAndSet(false, true)) {
                cleanup.run();
            }
        };
        emitter.onCompletion(onceOnly);
        emitter.onTimeout(onceOnly);
        emitter.onError(t -> onceOnly.run());
    }
}
