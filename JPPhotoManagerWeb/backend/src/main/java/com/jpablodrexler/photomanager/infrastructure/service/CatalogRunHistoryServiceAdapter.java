package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.CatalogRunHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogRunHistoryServiceAdapter implements CatalogRunHistoryPort {

    private static final String CATALOG_JOB_NAME = "catalogJob";

    private final JobExplorer jobExplorer;

    @Override
    public Optional<Instant> findLastCompletedCatalogRunTime() {
        return jobExplorer.getJobInstances(CATALOG_JOB_NAME, 0, 20)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(e -> e.getStatus() == BatchStatus.COMPLETED && e.getEndTime() != null)
                .max(Comparator.comparing(JobExecution::getEndTime))
                .map(e -> e.getEndTime().toInstant(ZoneOffset.UTC));
    }
}
