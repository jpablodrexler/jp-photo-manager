package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.CatalogRunState;

import java.time.Instant;
import java.util.Optional;

public interface CatalogStateRepository {

    Optional<CatalogRunState> find();

    boolean tryAcquire(String instanceId, Instant now);

    void release(String instanceId);

    void refreshHeartbeat(String instanceId, Instant now);

    boolean isStaleForInstance(String instanceId, Instant threshold);

    int releaseStaleForOtherInstances(String instanceId, Instant threshold);

    void markCompleted(String instanceId, Instant now);
}
