package com.jpablodrexler.photomanager.infrastructure.persistence.mongo;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoAssetExifRepository extends MongoRepository<AssetExifDocument, String> {

    Optional<AssetExifDocument> findByAssetId(Long assetId);

    void deleteByAssetId(Long assetId);
}
