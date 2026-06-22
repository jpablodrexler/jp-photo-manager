package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
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
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    }

    @Test
    void onCatalogProgress_progressMessage_sendsEventToEmitter() throws IOException {
        CatalogChangeNotification notification =
                new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, new Asset(), 0);
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
                new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, new Asset(), 0);
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
}
