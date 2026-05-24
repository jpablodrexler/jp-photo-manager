package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.SseNotificationRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final JobLauncher asyncCatalogJobLauncher;
    private final Job catalogJob;
    private final SseNotificationRegistry sseNotificationRegistry;
    private final Counter catalogAssetsCounter;

    public CatalogAssetsUseCaseImpl(
            @Qualifier("asyncCatalogJobLauncher") JobLauncher asyncCatalogJobLauncher,
            Job catalogJob,
            SseNotificationRegistry sseNotificationRegistry,
            MeterRegistry meterRegistry) {
        this.asyncCatalogJobLauncher = asyncCatalogJobLauncher;
        this.catalogJob = catalogJob;
        this.sseNotificationRegistry = sseNotificationRegistry;
        this.catalogAssetsCounter = Counter.builder("photomanager_catalog_assets_total")
                .description("Total assets cataloged")
                .register(meterRegistry);
    }

    @Override
    public CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener) {
        try {
            long runId = System.currentTimeMillis();
            CompletableFuture<Void> completion = new CompletableFuture<>();

            Consumer<CatalogChangeNotification> countingListener = listener == null ? null : notification -> {
                if (notification.getReason() == ReasonEnum.ASSET_CREATED) {
                    catalogAssetsCounter.increment();
                }
                listener.accept(notification);
            };

            sseNotificationRegistry.register(runId, countingListener, completion);

            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", runId)
                    .toJobParameters();

            JobExecution execution = asyncCatalogJobLauncher.run(catalogJob, params);
            log.debug("Started catalog job execution id={} runId={}", execution.getId(), runId);

            return completion;
        } catch (JobExecutionAlreadyRunningException e) {
            log.debug("Catalog already running, skipping");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to start catalog job", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
