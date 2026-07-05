package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifDocumentMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mongo.MongoAssetExifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetExifRepositoryImpl implements AssetExifRepository {

    private final MongoAssetExifRepository mongoRepository;
    private final AssetExifDocumentMapper mapper;

    @Override
    public Optional<AssetExif> findByAssetId(Long assetId) {
        return mongoRepository.findByAssetId(assetId).map(mapper::toDomain);
    }

    @Override
    public AssetExif save(AssetExif assetExif) {
        AssetExifDocument document = mapper.toDocument(assetExif);
        mongoRepository.findByAssetId(assetExif.getAssetId())
                .ifPresent(existing -> document.setId(existing.getId()));
        return mapper.toDomain(mongoRepository.save(document));
    }

    @Override
    public void deleteByAssetId(Long assetId) {
        mongoRepository.deleteByAssetId(assetId);
    }
}
