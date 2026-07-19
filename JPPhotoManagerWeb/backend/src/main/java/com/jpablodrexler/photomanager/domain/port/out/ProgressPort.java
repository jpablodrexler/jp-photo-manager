package com.jpablodrexler.photomanager.domain.port.out;

import java.util.concurrent.CompletableFuture;

public interface ProgressPort {
    void registerCompletion(long runId, CompletableFuture<Void> completion);
}
