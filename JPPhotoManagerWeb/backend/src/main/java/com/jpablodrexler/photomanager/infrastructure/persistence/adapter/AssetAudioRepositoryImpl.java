package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetAudioEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetAudioRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetAudioEntityMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetAudioRepositoryImpl implements AssetAudioRepository {

    private final JpaAssetAudioRepository jpa;
    private final AssetAudioEntityMapper mapper;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetAudio> findByAssetId(Long assetId) {
        return jpa.findByAssetAssetId(assetId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public AssetAudio save(AssetAudio assetAudio) {
        AssetAudioEntity entity = jpa.findByAssetAssetId(assetAudio.getAssetId())
                .orElseGet(AssetAudioEntity::new);
        entity.setAsset(entityManager.getReference(AssetEntity.class, assetAudio.getAssetId()));
        entity.setTitle(assetAudio.getTitle());
        entity.setArtist(assetAudio.getArtist());
        entity.setAlbum(assetAudio.getAlbum());
        entity.setDurationSeconds(assetAudio.getDurationSeconds());
        entity.setBitrateKbps(assetAudio.getBitrateKbps());
        entity.setSampleRateHz(assetAudio.getSampleRateHz());
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public void deleteByAssetId(Long assetId) {
        jpa.deleteByAssetAssetId(assetId);
    }
}
