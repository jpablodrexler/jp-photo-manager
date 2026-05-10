package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Page<Asset> findByFolder(Folder folder, Pageable pageable);

    List<Asset> findByFolder(Folder folder);

    List<Asset> findAll();

    Optional<Asset> findByFolderAndFileName(Folder folder, String fileName);

    boolean existsByFolderAndFileName(Folder folder, String fileName);

    void deleteByFolderAndFileName(Folder folder, String fileName);

    Page<Asset> findByFolderAndDeletedAtIsNull(Folder folder, Pageable pageable);

    List<Asset> findByFolderAndDeletedAtIsNull(Folder folder);

    @Query("SELECT a FROM Asset a JOIN FETCH a.folder WHERE a.deletedAt IS NOT NULL ORDER BY a.deletedAt DESC")
    Page<Asset> findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Asset a JOIN FETCH a.folder WHERE a.deletedAt < :cutoff AND a.deletedAt IS NOT NULL")
    List<Asset> findByDeletedAtBeforeAndDeletedAtIsNotNull(LocalDateTime cutoff);

    @Query("SELECT a FROM Asset a JOIN FETCH a.folder WHERE a.deletedAt IS NULL")
    List<Asset> findByDeletedAtIsNull();

    @Query("SELECT a FROM Asset a JOIN FETCH a.folder WHERE a.hash = :hash AND a.deletedAt IS NULL")
    List<Asset> findByHashAndDeletedAtIsNull(String hash);

    @Query("SELECT a FROM Asset a JOIN FETCH a.folder WHERE a.hash = :hash")
    List<Asset> findByHash(String hash);

    @Query("SELECT a FROM Asset a GROUP BY a.hash HAVING COUNT(a) > 1")
    List<String> findDuplicateHashes();

    @Query("SELECT aa FROM Album a JOIN a.assets aa JOIN FETCH aa.folder WHERE a.albumId = :albumId")
    Page<Asset> findByAlbumId(@Param("albumId") Long albumId, Pageable pageable);

    @Query("""
            SELECT a FROM Asset a
            JOIN FETCH a.folder
            WHERE a.folder = :folder
              AND a.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(a.fileName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:dateFrom IS NULL OR a.fileCreationDateTime >= :dateFrom)
              AND (:dateTo   IS NULL OR a.fileCreationDateTime <= :dateTo)
              AND (:minRating IS NULL OR a.rating >= :minRating)
            """)
    Page<Asset> findByFolderWithFilters(
            @Param("folder") Folder folder,
            @Param("search") String search,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("minRating") Integer minRating,
            Pageable pageable);
}
