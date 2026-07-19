package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

public interface AssetSummary {
    Long getAssetId();
    String getFileName();
    String getFolderPath();
    long getFileSize();

    default String getThumbnailUrl() {
        return "/api/assets/" + getAssetId() + "/thumbnail";
    }
}
