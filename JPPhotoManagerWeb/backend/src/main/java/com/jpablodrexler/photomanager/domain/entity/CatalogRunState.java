package com.jpablodrexler.photomanager.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "catalog_run_state")
@Data
@NoArgsConstructor
public class CatalogRunState {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "is_running", nullable = false)
    private boolean running;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "instance_id")
    private String instanceId;
}
