package com.jpablodrexler.photomanager.domain.port.in.convert;

import java.util.concurrent.CompletableFuture;

public interface ConvertAssetsUseCase {
    CompletableFuture<Void> execute(long runId);
}
