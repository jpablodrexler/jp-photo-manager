package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAssetExifRepository extends JpaRepository<AssetExifEntity, Long> {

    Optional<AssetExifEntity> findByAssetAssetId(Long assetId);

    void deleteByAssetAssetId(Long assetId);
}
