package com.jpablodrexler.photomanager.application.dto;

import java.util.List;

public record SyncProgressMessage(long runId, String status, List<SyncAssetsResult> results, boolean done) {

    public static SyncProgressMessage progress(long runId, String status) {
        return new SyncProgressMessage(runId, status, null, false);
    }

    public static SyncProgressMessage done(long runId, List<SyncAssetsResult> results) {
        return new SyncProgressMessage(runId, null, results, true);
    }
}
