package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

public interface AssetSummary {
    Long getAssetId();
    String getFileName();
    String getFolderPath();

    default String getThumbnailUrl() {
        return "/api/assets/" + getAssetId() + "/thumbnail";
    }
}
