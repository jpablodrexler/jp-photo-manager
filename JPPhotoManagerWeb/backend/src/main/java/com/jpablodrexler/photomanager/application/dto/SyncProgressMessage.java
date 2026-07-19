package com.jpablodrexler.photomanager.application.dto;

import java.util.List;
import java.util.UUID;

public record SyncProgressMessage(long runId, String status, List<SyncAssetsResult> results, boolean done,
        UUID userId) {

    public static SyncProgressMessage progress(long runId, String status) {
        return new SyncProgressMessage(runId, status, null, false, null);
    }

    public static SyncProgressMessage done(long runId, List<SyncAssetsResult> results, UUID userId) {
        return new SyncProgressMessage(runId, null, results, true, userId);
    }
}
