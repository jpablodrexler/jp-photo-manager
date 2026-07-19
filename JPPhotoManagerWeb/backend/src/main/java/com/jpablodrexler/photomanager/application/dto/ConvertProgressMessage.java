package com.jpablodrexler.photomanager.application.dto;

import java.util.List;
import java.util.UUID;

public record ConvertProgressMessage(long runId, String status, List<ConvertAssetsResult> results, boolean done,
        UUID userId) {

    public static ConvertProgressMessage progress(long runId, String status) {
        return new ConvertProgressMessage(runId, status, null, false, null);
    }

    public static ConvertProgressMessage done(long runId, List<ConvertAssetsResult> results, UUID userId) {
        return new ConvertProgressMessage(runId, null, results, true, userId);
    }
}
