package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogRunState {

    private Integer id;
    private boolean running;
    private Instant startedAt;
    private Instant lastHeartbeatAt;
    private String instanceId;
    private Instant lastCompletedAt;
}
