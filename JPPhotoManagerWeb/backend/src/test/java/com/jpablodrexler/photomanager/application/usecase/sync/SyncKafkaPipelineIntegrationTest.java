package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.PostgresIntegrationTest;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.CatalogScheduler;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = {
    "job.catalog.progress", "job.sync.progress", "job.convert.progress",
    "asset.cataloged", "asset.deleted"
})
class SyncKafkaPipelineIntegrationTest extends PostgresIntegrationTest {

    @MockitoBean
    CatalogScheduler catalogScheduler;

    @Autowired
    SyncAssetsUseCase syncAssetsUseCase;

    @Autowired
    KafkaProgressRegistry kafkaProgressRegistry;

    @Test
    void syncUseCase_noDefinitions_completesRegisteredFutureViaKafka() throws Exception {
        long runId = System.currentTimeMillis();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        kafkaProgressRegistry.registerCompletion(runId, completion);

        syncAssetsUseCase.execute(runId);

        completion.get(10, TimeUnit.SECONDS);
        assertThat(completion.isDone()).isTrue();
        assertThat(completion.isCompletedExceptionally()).isFalse();
    }

}
