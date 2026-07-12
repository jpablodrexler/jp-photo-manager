package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface CatalogFolderPort {

    void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                       Runnable heartbeatCallback, AtomicInteger processed, int total);

    Asset createAsset(String directoryPath, String fileName);
}
