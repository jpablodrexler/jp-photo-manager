package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository {

    List<Album> findByUserId(UUID userId);

    Optional<Album> findByIdAndUserId(Long albumId, UUID userId);

    Album save(Album album);

    void deleteById(Long albumId);

    PaginatedResult<Asset> findAssetsByAlbumId(Long albumId, int page, int pageSize);

    long countAssets(Long albumId);

    void addAssets(Long albumId, List<Long> assetIds);

    void removeAssets(Long albumId, List<Long> assetIds);

    PaginatedResult<Asset> findSmartAlbumAssets(AssetFilter filter, int page, int pageSize);
}
