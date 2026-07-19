package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAssetAudioRepository extends JpaRepository<AssetAudioEntity, Long> {

    Optional<AssetAudioEntity> findByAssetAssetId(Long assetId);

    void deleteByAssetAssetId(Long assetId);
}
