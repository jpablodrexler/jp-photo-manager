package com.jpablodrexler.photomanager.application.dto;

public record CatalogProgressMessage(long runId, CatalogChangeNotification notification, boolean done,
        Integer foldersScanned, Long assetsAdded, Long durationMs) {

    public static CatalogProgressMessage progress(long runId, CatalogChangeNotification notification) {
        return new CatalogProgressMessage(runId, notification, false, null, null, null);
    }

    public static CatalogProgressMessage done(long runId) {
        return new CatalogProgressMessage(runId, null, true, null, null, null);
    }

    public static CatalogProgressMessage done(long runId, int foldersScanned, long assetsAdded, long durationMs) {
        return new CatalogProgressMessage(runId, null, true, foldersScanned, assetsAdded, durationMs);
    }
}
