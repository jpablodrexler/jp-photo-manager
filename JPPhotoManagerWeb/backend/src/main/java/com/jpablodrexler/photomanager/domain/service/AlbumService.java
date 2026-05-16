package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumService {

    List<Album> findByUserId(UUID userId);

    Optional<Album> findByIdAndUserId(Long albumId, UUID userId);

    Album createAlbum(UUID userId, String name, String description);

    Album updateAlbum(Long albumId, UUID userId, String name, String description);

    void deleteAlbum(Long albumId, UUID userId);

    PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex);

    void addAssets(Long albumId, UUID userId, List<Long> assetIds);

    void removeAssets(Long albumId, UUID userId, List<Long> assetIds);
}
