package com.jpablodrexler.photomanager.domain.port.out;

import java.time.Instant;
import java.util.Optional;

public interface CatalogRunHistoryPort {

    Optional<Instant> findLastCompletedCatalogRunTime();
}
