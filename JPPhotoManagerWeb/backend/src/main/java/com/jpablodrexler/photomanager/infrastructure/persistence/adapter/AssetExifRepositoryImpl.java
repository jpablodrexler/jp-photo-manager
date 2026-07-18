package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifEntityMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetExifRepositoryImpl implements AssetExifRepository {

    private final JpaAssetExifRepository jpa;
    private final AssetExifEntityMapper mapper;
    private final EntityManager entityManager;

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
        entity.setAsset(entityManager.getReference(AssetEntity.class, assetExif.getAssetId()));
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
        entity.setRawExif(assetExif.getRawExif());
        AssetExifEntity saved = jpa.save(entity);

        // Callers (e.g. AssetExifProcessor) read via the readOnly-transactional findByAssetId()
        // above in the same physical transaction before calling save(). Spring leaves the
        // Hibernate session's flush mode set to MANUAL after that nested readOnly participation,
        // and MANUAL means Hibernate will not auto-flush even at commit — so without an explicit
        // flush here, this insert/update is silently never written, while sibling bulk @Modifying
        // queries on other repositories (which bypass the persistence context) keep working,
        // masking the loss (confirmed via SQL logging: no INSERT INTO asset_exif was ever emitted
        // without this call).
        entityManager.flush();
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByAssetId(Long assetId) {
        jpa.deleteByAssetAssetId(assetId);
    }
}
