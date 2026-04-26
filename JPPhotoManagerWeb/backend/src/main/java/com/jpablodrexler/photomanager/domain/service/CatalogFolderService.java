package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.entity.Asset;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface CatalogFolderService {

    void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                       AtomicInteger processed, int total);

    Asset createAsset(String directoryPath, String fileName);
}
