package com.jpablodrexler.photomanager.application.dto;

public record CatalogProgressMessage(long runId, CatalogChangeNotification notification, boolean done) {

    public static CatalogProgressMessage progress(long runId, CatalogChangeNotification notification) {
        return new CatalogProgressMessage(runId, notification, false);
    }

    public static CatalogProgressMessage done(long runId) {
        return new CatalogProgressMessage(runId, null, true);
    }
}
