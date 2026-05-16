package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetExifRepositoryImpl implements AssetExifRepository {

    private final JpaAssetExifRepository jpa;
    private final AssetExifEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetExif> findByAssetId(Long assetId) {
        return jpa.findByAssetAssetId(assetId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public AssetExif save(AssetExif assetExif) {
        AssetExifEntity entity = jpa.findByAssetAssetId(assetExif.getAssetId())
                .orElseGet(AssetExifEntity::new);
        AssetEntity assetRef = new AssetEntity();
        assetRef.setAssetId(assetExif.getAssetId());
        entity.setAsset(assetRef);
        entity.setCameraMake(assetExif.getCameraMake());
        entity.setCameraModel(assetExif.getCameraModel());
        entity.setLensModel(assetExif.getLensModel());
        entity.setExposureTime(assetExif.getExposureTime());
        entity.setFNumber(assetExif.getFNumber());
        entity.setIsoSpeed(assetExif.getIsoSpeed());
        entity.setFocalLength(assetExif.getFocalLength());
        entity.setDateTaken(assetExif.getDateTaken());
        entity.setWidthPixels(assetExif.getWidthPixels());
        entity.setHeightPixels(assetExif.getHeightPixels());
        entity.setGpsLatitude(assetExif.getGpsLatitude());
        entity.setGpsLongitude(assetExif.getGpsLongitude());
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public void deleteByAssetId(Long assetId) {
        jpa.deleteByAssetAssetId(assetId);
    }
}
