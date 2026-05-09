package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByUser_Id(UUID userId);

    Optional<Album> findByAlbumIdAndUser_Id(Long albumId, UUID userId);

    @Query("SELECT COUNT(aa) FROM Album a JOIN a.assets aa WHERE a.albumId = :albumId")
    long countAssets(@Param("albumId") Long albumId);
}
