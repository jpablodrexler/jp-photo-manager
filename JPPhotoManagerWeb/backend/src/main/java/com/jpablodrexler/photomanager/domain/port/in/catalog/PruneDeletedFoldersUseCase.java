package com.jpablodrexler.photomanager.domain.port.in.catalog;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;

import java.util.function.Consumer;

public interface PruneDeletedFoldersUseCase {
    void execute(Consumer<CatalogChangeNotification> consumer);
}
