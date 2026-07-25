package com.jpablodrexler.photomanager.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SseCleanupTest {

    @Test
    void registerOnce_onCompletionFires_runsCleanupOnce() {
        SseEmitter emitter = mock(SseEmitter.class);
        Runnable cleanup = mock(Runnable.class);
        ArgumentCaptor<Runnable> onCompletionCaptor = ArgumentCaptor.forClass(Runnable.class);

        SseCleanup.registerOnce(emitter, cleanup);

        verify(emitter).onCompletion(onCompletionCaptor.capture());
        onCompletionCaptor.getValue().run();

        verify(cleanup, times(1)).run();
    }

    @Test
    void registerOnce_completionThenErrorBothFire_runsCleanupExactlyOnce() {
        SseEmitter emitter = mock(SseEmitter.class);
        Runnable cleanup = mock(Runnable.class);
        ArgumentCaptor<Runnable> onCompletionCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onErrorCaptor = ArgumentCaptor.forClass(Consumer.class);

        SseCleanup.registerOnce(emitter, cleanup);

        verify(emitter).onCompletion(onCompletionCaptor.capture());
        verify(emitter).onError(onErrorCaptor.capture());

        // Simulates Spring invoking both terminal callbacks for the same connection — the exact
        // condition that drove photomanager_active_sse_connections negative in production.
        onCompletionCaptor.getValue().run();
        onErrorCaptor.getValue().accept(new RuntimeException("broken pipe"));

        verify(cleanup, times(1)).run();
    }

    @Test
    void registerOnce_allThreeCallbacksFire_runsCleanupExactlyOnce() {
        SseEmitter emitter = mock(SseEmitter.class);
        Runnable cleanup = mock(Runnable.class);
        ArgumentCaptor<Runnable> onCompletionCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> onTimeoutCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onErrorCaptor = ArgumentCaptor.forClass(Consumer.class);

        SseCleanup.registerOnce(emitter, cleanup);

        verify(emitter).onCompletion(onCompletionCaptor.capture());
        verify(emitter).onTimeout(onTimeoutCaptor.capture());
        verify(emitter).onError(onErrorCaptor.capture());

        onTimeoutCaptor.getValue().run();
        onCompletionCaptor.getValue().run();
        onErrorCaptor.getValue().accept(new RuntimeException("broken pipe"));

        verify(cleanup, times(1)).run();
    }

    @Test
    void registerOnce_registersAllThreeTerminalCallbacks() {
        SseEmitter emitter = mock(SseEmitter.class);

        SseCleanup.registerOnce(emitter, mock(Runnable.class));

        verify(emitter).onCompletion(any());
        verify(emitter).onTimeout(any());
        verify(emitter).onError(any());
    }
}
