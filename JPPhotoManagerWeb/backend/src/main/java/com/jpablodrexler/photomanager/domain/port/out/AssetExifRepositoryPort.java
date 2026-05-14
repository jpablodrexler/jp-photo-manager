package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.AssetExif;

import java.util.Optional;

public interface AssetExifRepositoryPort {
    Optional<AssetExif> findByAssetId(Long assetId);
    AssetExif save(AssetExif assetExif);
    void deleteByAssetId(Long assetId);
}
