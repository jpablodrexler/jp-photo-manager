package com.jpablodrexler.photomanager.domain.port.in.catalog;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CatalogAssetsUseCase {
    CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener);
}
