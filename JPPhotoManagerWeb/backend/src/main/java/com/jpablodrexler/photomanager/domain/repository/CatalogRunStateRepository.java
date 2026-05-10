package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.CatalogRunState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface CatalogRunStateRepository extends JpaRepository<CatalogRunState, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE CatalogRunState s SET s.running = true, s.startedAt = :now, s.lastHeartbeatAt = :now, s.instanceId = :instanceId WHERE s.id = 1 AND s.running = false")
    int tryAcquire(String instanceId, Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE CatalogRunState s SET s.running = false, s.startedAt = null, s.lastHeartbeatAt = null, s.instanceId = null WHERE s.id = 1 AND s.instanceId = :instanceId")
    void release(String instanceId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE CatalogRunState s SET s.lastHeartbeatAt = :now WHERE s.id = 1 AND s.instanceId = :instanceId AND s.running = true")
    void refreshHeartbeat(String instanceId, Instant now);

    @Query("SELECT COUNT(s) > 0 FROM CatalogRunState s WHERE s.id = 1 AND s.running = true AND s.instanceId = :instanceId AND s.lastHeartbeatAt < :threshold")
    boolean isStaleForInstance(String instanceId, Instant threshold);

    @Modifying
    @Transactional
    @Query("UPDATE CatalogRunState s SET s.running = false, s.startedAt = null, s.lastHeartbeatAt = null, s.instanceId = null WHERE s.id = 1 AND s.running = true AND s.instanceId != :instanceId AND s.lastHeartbeatAt < :threshold")
    int releaseStaleForOtherInstances(String instanceId, Instant threshold);

    @Modifying
    @Transactional
    @Query("UPDATE CatalogRunState s SET s.lastCompletedAt = :now WHERE s.id = 1 AND s.instanceId = :instanceId")
    void markCompleted(String instanceId, Instant now);
}
