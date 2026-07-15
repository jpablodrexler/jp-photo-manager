package com.jpablodrexler.photomanager.domain.port.in.folder;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;

import java.util.function.Consumer;

public interface PruneDeletedFoldersUseCase {
    void execute(Consumer<CatalogChangeNotification> consumer);
}
