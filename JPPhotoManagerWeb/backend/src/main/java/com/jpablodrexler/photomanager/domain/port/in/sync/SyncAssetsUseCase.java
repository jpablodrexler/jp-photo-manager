package com.jpablodrexler.photomanager.domain.port.in.sync;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SyncAssetsUseCase {
    CompletableFuture<Void> execute(long runId, UUID userId);
}
