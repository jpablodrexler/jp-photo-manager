package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;

public record CatalogBatchItem(Asset asset, byte[] thumbnailData, AssetExif assetExif, AssetAudio assetAudio) {}
