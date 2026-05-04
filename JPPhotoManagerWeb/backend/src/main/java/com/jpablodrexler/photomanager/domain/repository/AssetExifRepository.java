package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.AssetExif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetExifRepository extends JpaRepository<AssetExif, Long> {

    Optional<AssetExif> findByAssetAssetId(Long assetId);
}
