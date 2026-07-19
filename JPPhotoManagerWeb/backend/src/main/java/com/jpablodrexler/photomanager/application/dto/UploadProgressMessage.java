package com.jpablodrexler.photomanager.application.dto;

import com.jpablodrexler.photomanager.domain.enums.UploadStage;

public record UploadProgressMessage(Long assetId, UploadStage stage, boolean done, boolean failed) {

    public static UploadProgressMessage stageComplete(Long assetId, UploadStage stage) {
        return new UploadProgressMessage(assetId, stage, false, false);
    }

    public static UploadProgressMessage done(Long assetId) {
        return new UploadProgressMessage(assetId, null, true, false);
    }

    public static UploadProgressMessage failed(Long assetId, UploadStage stage) {
        return new UploadProgressMessage(assetId, stage, true, true);
    }
}
