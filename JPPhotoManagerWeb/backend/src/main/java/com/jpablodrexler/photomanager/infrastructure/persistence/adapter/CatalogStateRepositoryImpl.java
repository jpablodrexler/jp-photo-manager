package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.CatalogRunState;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.CatalogRunStateEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaCatalogStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogStateRepositoryImpl implements CatalogStateRepository {

    private final JpaCatalogStateRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogRunState> find() {
        return jpa.findById(1).map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean tryAcquire(String instanceId, Instant now) {
        return jpa.tryAcquire(instanceId, now) > 0;
    }

    @Override
    @Transactional
    public void release(String instanceId) {
        jpa.release(instanceId);
    }

    @Override
    @Transactional
    public void refreshHeartbeat(String instanceId, Instant now) {
        jpa.refreshHeartbeat(instanceId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStaleForInstance(String instanceId, Instant threshold) {
        return jpa.isStaleForInstance(instanceId, threshold);
    }

    @Override
    @Transactional
    public int releaseStaleForOtherInstances(String instanceId, Instant threshold) {
        return jpa.releaseStaleForOtherInstances(instanceId, threshold);
    }

    @Override
    @Transactional
    public void markCompleted(String instanceId, Instant now) {
        jpa.markCompleted(instanceId, now);
    }

    private CatalogRunState toDomain(CatalogRunStateEntity e) {
        return CatalogRunState.builder()
                .id(e.getId())
                .running(e.isRunning())
                .startedAt(e.getStartedAt())
                .lastHeartbeatAt(e.getLastHeartbeatAt())
                .instanceId(e.getInstanceId())
                .lastCompletedAt(e.getLastCompletedAt())
                .build();
    }
}
