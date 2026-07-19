package com.jpablodrexler.photomanager.domain.port.in.convert;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ConvertAssetsUseCase {
    CompletableFuture<Void> execute(long runId, UUID userId);
}
