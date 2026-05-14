package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepositoryPort {
    List<Album> findByUserId(UUID userId);
    Optional<Album> findByIdAndUserId(Long albumId, UUID userId);
    PaginatedResult<Asset> findAlbumAssets(Long albumId, int page, int pageSize);
    long countAssets(Long albumId);
    Album save(Album album);
    void delete(Album album);
    void addAssets(Long albumId, List<Long> assetIds);
    void removeAssets(Long albumId, List<Long> assetIds);
}
