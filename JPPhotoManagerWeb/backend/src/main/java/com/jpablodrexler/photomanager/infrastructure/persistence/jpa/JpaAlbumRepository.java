package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AlbumEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAlbumRepository extends JpaRepository<AlbumEntity, Long> {

    List<AlbumEntity> findByUser_Id(UUID userId);

    Optional<AlbumEntity> findByAlbumIdAndUser_Id(Long albumId, UUID userId);

    @Query("SELECT COUNT(aa) FROM AlbumEntity a JOIN a.assets aa WHERE a.albumId = :albumId")
    long countAssets(@Param("albumId") Long albumId);

    @Query("SELECT aa FROM AlbumEntity a JOIN a.assets aa JOIN FETCH aa.folder WHERE a.albumId = :albumId")
    Page<AssetEntity> findAssetsByAlbumId(@Param("albumId") Long albumId, Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO album_assets (album_id, asset_id) VALUES (:albumId, :assetId)", nativeQuery = true)
    void addAsset(@Param("albumId") Long albumId, @Param("assetId") Long assetId);

    @Modifying
    @Query(value = "DELETE FROM album_assets WHERE album_id = :albumId AND asset_id = :assetId", nativeQuery = true)
    void removeAsset(@Param("albumId") Long albumId, @Param("assetId") Long assetId);
}
