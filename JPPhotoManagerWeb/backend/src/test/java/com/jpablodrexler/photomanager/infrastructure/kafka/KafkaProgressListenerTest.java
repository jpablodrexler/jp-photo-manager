package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.application.dto.ConvertProgressMessage;
import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.Reason;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProgressListenerTest {

    @Mock KafkaProgressRegistry registry;
    @Mock SseEmitter emitter;

    @InjectMocks KafkaProgressListener sut;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getEmitter(42L)).thenReturn(emitter);
        lenient().when(registry.getCatalogObservers()).thenReturn(Set.of());
    }

    @Test
    void onCatalogProgress_progressMessage_sendsEventToEmitter() throws IOException {
        CatalogChangeNotification notification =
                new CatalogChangeNotification(Reason.ASSET_CREATED, new Asset(), 0);
        CatalogProgressMessage message = CatalogProgressMessage.progress(42L, notification);

        sut.onCatalogProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
    }

    @Test
    void onCatalogProgress_doneMessage_completesEmitterAndRegistry() {
        CatalogProgressMessage message = CatalogProgressMessage.done(42L);

        sut.onCatalogProgress(message);

        verify(emitter).complete();
        verify(registry).complete(42L);
        verify(registry).remove(42L);
    }

    @Test
    void onCatalogProgress_unknownRunId_silentlySkips() throws IOException {
        when(registry.getEmitter(99L)).thenReturn(null);
        CatalogChangeNotification notification =
                new CatalogChangeNotification(Reason.ASSET_CREATED, new Asset(), 0);
        CatalogProgressMessage message = CatalogProgressMessage.progress(99L, notification);

        sut.onCatalogProgress(message);

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onCatalogProgress_doneMessage_unknownRunId_stillCompletesRegistry() {
        when(registry.getEmitter(99L)).thenReturn(null);
        CatalogProgressMessage message = CatalogProgressMessage.done(99L);

        sut.onCatalogProgress(message);

        verify(registry).complete(99L);
        verify(registry).remove(99L);
    }

    // --- onSyncProgress ---

    @Test
    void onSyncProgress_progressMessage_sendsStatusEventToEmitter() throws IOException {
        SyncProgressMessage message = SyncProgressMessage.progress(42L, "Syncing files...");

        sut.onSyncProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
    }

    @Test
    void onSyncProgress_doneMessage_sendsResultsEventAndCompletesEmitter() throws IOException {
        SyncProgressMessage message = SyncProgressMessage.done(42L, List.of(), null);

        sut.onSyncProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        verify(registry).complete(42L);
        verify(registry).remove(42L);
    }

    @Test
    void onSyncProgress_unknownRunId_progressMessage_silentlySkips() throws IOException {
        when(registry.getEmitter(99L)).thenReturn(null);
        SyncProgressMessage message = SyncProgressMessage.progress(99L, "Syncing...");

        sut.onSyncProgress(message);

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onSyncProgress_doneMessage_unknownRunId_stillCompletesRegistry() {
        when(registry.getEmitter(99L)).thenReturn(null);
        SyncProgressMessage message = SyncProgressMessage.done(99L, List.of(), null);

        sut.onSyncProgress(message);

        verify(registry).complete(99L);
        verify(registry).remove(99L);
    }

    // --- onConvertProgress ---

    @Test
    void onConvertProgress_progressMessage_sendsStatusEventToEmitter() throws IOException {
        ConvertProgressMessage message = ConvertProgressMessage.progress(42L, "Converting files...");

        sut.onConvertProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
    }

    @Test
    void onConvertProgress_doneMessage_sendsResultsEventAndCompletesEmitter() throws IOException {
        ConvertProgressMessage message = ConvertProgressMessage.done(42L, List.of(), null);

        sut.onConvertProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        verify(registry).complete(42L);
        verify(registry).remove(42L);
    }

    @Test
    void onConvertProgress_unknownRunId_progressMessage_silentlySkips() throws IOException {
        when(registry.getEmitter(99L)).thenReturn(null);
        ConvertProgressMessage message = ConvertProgressMessage.progress(99L, "Converting...");

        sut.onConvertProgress(message);

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onConvertProgress_doneMessage_unknownRunId_stillCompletesRegistry() {
        when(registry.getEmitter(99L)).thenReturn(null);
        ConvertProgressMessage message = ConvertProgressMessage.done(99L, List.of(), null);

        sut.onConvertProgress(message);

        verify(registry).complete(99L);
        verify(registry).remove(99L);
    }

    // --- onUploadProgress ---

    @Test
    void onUploadProgress_noObserverConnected_doesNothing() {
        when(registry.getEmitter(7L)).thenReturn(null);
        UploadProgressMessage message = UploadProgressMessage.stageComplete(7L, UploadStage.HASH);

        sut.onUploadProgress(message);

        verify(registry, never()).remove(anyLong());
    }

    @Test
    void onUploadProgress_stageUpdate_sendsStageEvent() throws IOException {
        when(registry.getEmitter(7L)).thenReturn(emitter);
        UploadProgressMessage message = UploadProgressMessage.stageComplete(7L, UploadStage.HASH);

        sut.onUploadProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(registry, never()).remove(7L);
    }

    @Test
    void onUploadProgress_doneSuccessfully_sendsDoneEventCompletesAndRemoves() throws IOException {
        when(registry.getEmitter(7L)).thenReturn(emitter);
        UploadProgressMessage message = UploadProgressMessage.done(7L);

        sut.onUploadProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        verify(registry).remove(7L);
    }

    @Test
    void onUploadProgress_doneFailed_sendsFailedEventCompletesAndRemoves() throws IOException {
        when(registry.getEmitter(7L)).thenReturn(emitter);
        UploadProgressMessage message = UploadProgressMessage.failed(7L, UploadStage.EXIF);

        sut.onUploadProgress(message);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        verify(registry).remove(7L);
    }

    @Test
    void onUploadProgress_stageUpdateSendFails_doesNotThrow() throws IOException {
        when(registry.getEmitter(7L)).thenReturn(emitter);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        UploadProgressMessage message = UploadProgressMessage.stageComplete(7L, UploadStage.HASH);

        sut.onUploadProgress(message);

        verify(emitter, never()).complete();
    }

    @Test
    void onUploadProgress_doneSendFails_stillCompletesAndRemoves() throws IOException {
        when(registry.getEmitter(7L)).thenReturn(emitter);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        UploadProgressMessage message = UploadProgressMessage.done(7L);

        sut.onUploadProgress(message);

        verify(emitter).complete();
        verify(registry).remove(7L);
    }

    // --- catalog observer broadcast ---

    @Test
    void onCatalogProgress_progressMessage_broadcastsToEachRegisteredObserver() throws IOException {
        SseEmitter observer1 = mock(SseEmitter.class);
        SseEmitter observer2 = mock(SseEmitter.class);
        when(registry.getCatalogObservers()).thenReturn(new LinkedHashSet<>(List.of(observer1, observer2)));
        CatalogChangeNotification notification =
                new CatalogChangeNotification(Reason.ASSET_CREATED, new Asset(), 0);
        CatalogProgressMessage message = CatalogProgressMessage.progress(42L, notification);

        sut.onCatalogProgress(message);

        verify(observer1).send(any(SseEmitter.SseEventBuilder.class));
        verify(observer2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onCatalogProgress_progressMessage_observerSendFails_removesThatObserver() throws IOException {
        SseEmitter failingObserver = mock(SseEmitter.class);
        when(registry.getCatalogObservers()).thenReturn(new LinkedHashSet<>(List.of(failingObserver)));
        doThrow(new IOException("gone")).when(failingObserver).send(any(SseEmitter.SseEventBuilder.class));
        CatalogChangeNotification notification =
                new CatalogChangeNotification(Reason.ASSET_CREATED, new Asset(), 0);
        CatalogProgressMessage message = CatalogProgressMessage.progress(42L, notification);

        sut.onCatalogProgress(message);

        verify(registry).removeCatalogObserver(failingObserver);
    }

    @Test
    void onCatalogProgress_doneMessage_broadcastsCatalogDoneToEachObserver() throws IOException {
        SseEmitter observer = mock(SseEmitter.class);
        when(registry.getCatalogObservers()).thenReturn(new LinkedHashSet<>(List.of(observer)));
        CatalogProgressMessage message = CatalogProgressMessage.done(42L);

        sut.onCatalogProgress(message);

        verify(observer).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onCatalogProgress_doneMessage_observerSendFails_removesThatObserver() throws IOException {
        SseEmitter failingObserver = mock(SseEmitter.class);
        when(registry.getCatalogObservers()).thenReturn(new LinkedHashSet<>(List.of(failingObserver)));
        doThrow(new IOException("gone")).when(failingObserver).send(any(SseEmitter.SseEventBuilder.class));
        CatalogProgressMessage message = CatalogProgressMessage.done(42L);

        sut.onCatalogProgress(message);

        verify(registry).removeCatalogObserver(failingObserver);
    }
}
