package com.jpablodrexler.photomanager.application.dto;

import java.util.List;

public record ConvertProgressMessage(long runId, String status, List<ConvertAssetsResult> results, boolean done) {

    public static ConvertProgressMessage progress(long runId, String status) {
        return new ConvertProgressMessage(runId, status, null, false);
    }

    public static ConvertProgressMessage done(long runId, List<ConvertAssetsResult> results) {
        return new ConvertProgressMessage(runId, null, results, true);
    }
}
