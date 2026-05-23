package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.AssetAudio;

import java.util.Optional;

public interface AssetAudioRepository {

    Optional<AssetAudio> findByAssetId(Long assetId);

    AssetAudio save(AssetAudio assetAudio);

    void deleteByAssetId(Long assetId);
}
