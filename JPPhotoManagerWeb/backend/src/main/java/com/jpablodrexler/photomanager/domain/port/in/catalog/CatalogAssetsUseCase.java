package com.jpablodrexler.photomanager.domain.port.in.catalog;

import java.util.concurrent.CompletableFuture;

public interface CatalogAssetsUseCase {
    CompletableFuture<Void> execute(long runId);
}
