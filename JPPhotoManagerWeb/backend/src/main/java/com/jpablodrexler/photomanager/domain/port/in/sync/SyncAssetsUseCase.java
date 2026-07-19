package com.jpablodrexler.photomanager.domain.port.in.sync;

import java.util.concurrent.CompletableFuture;

public interface SyncAssetsUseCase {
    CompletableFuture<Void> execute(long runId);
}
