package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.CatalogRunState;

import java.time.Instant;
import java.util.Optional;

public interface CatalogStateRepositoryPort {
    Optional<CatalogRunState> findById(Integer id);
    boolean tryAcquire(String instanceId, Instant now);
    void release(String instanceId);
    void refreshHeartbeat(String instanceId, Instant now);
    void markCompleted(String instanceId, Instant now);
    int releaseStaleForOtherInstances(String instanceId, Instant threshold);
}
