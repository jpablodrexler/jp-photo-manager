package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CatalogAssetsService {

    void runCatalog();

    CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback);

    Asset createAsset(String directoryPath, String fileName);
}
