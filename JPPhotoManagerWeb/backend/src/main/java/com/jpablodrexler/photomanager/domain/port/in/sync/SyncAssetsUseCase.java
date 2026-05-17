package com.jpablodrexler.photomanager.domain.port.in.sync;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SyncAssetsUseCase {
    CompletableFuture<List<SyncAssetsResult>> execute(Consumer<String> listener);
}
