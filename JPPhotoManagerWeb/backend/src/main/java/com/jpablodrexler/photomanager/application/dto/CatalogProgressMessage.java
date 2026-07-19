package com.jpablodrexler.photomanager.application.dto;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;

import java.util.UUID;

public record CatalogProgressMessage(long runId, CatalogChangeNotification notification, boolean done,
        Integer foldersScanned, Long assetsAdded, Long durationMs, UUID userId) {

    public static CatalogProgressMessage progress(long runId, CatalogChangeNotification notification) {
        return new CatalogProgressMessage(runId, notification, false, null, null, null, null);
    }

    public static CatalogProgressMessage done(long runId) {
        return new CatalogProgressMessage(runId, null, true, null, null, null, null);
    }

    public static CatalogProgressMessage done(long runId, int foldersScanned, long assetsAdded, long durationMs,
            UUID userId) {
        return new CatalogProgressMessage(runId, null, true, foldersScanned, assetsAdded, durationMs, userId);
    }
}
