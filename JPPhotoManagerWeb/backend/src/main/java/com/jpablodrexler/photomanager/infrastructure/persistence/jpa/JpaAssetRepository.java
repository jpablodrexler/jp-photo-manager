package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface JpaAssetRepository extends JpaRepository<AssetEntity, Long>, JpaSpecificationExecutor<AssetEntity> {

    List<AssetEntity> findByFolder(FolderEntity folder);

    Optional<AssetEntity> findByFolderAndFileName(FolderEntity folder, String fileName);

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.fileName = :fileName AND a.deletedAt IS NULL")
    List<AssetEntity> findByFileNameNotDeleted(@Param("fileName") String fileName);

    boolean existsByFolderAndFileName(FolderEntity folder, String fileName);

    void deleteByFolderAndFileName(FolderEntity folder, String fileName);

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt IS NOT NULL ORDER BY a.deletedAt DESC")
    Page<AssetEntity> findDeletedOrderByDeletedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt IS NULL")
    List<AssetEntity> findNotDeleted();

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.hash = :hash AND a.deletedAt IS NULL")
    List<AssetEntity> findByHashNotDeleted(String hash);

    @Query("SELECT a.hash FROM AssetEntity a WHERE a.hash IS NOT NULL GROUP BY a.hash HAVING COUNT(a) > 1")
    List<String> findDuplicateHashes();

    @Query("SELECT aa FROM AlbumEntity a JOIN a.assets aa JOIN FETCH aa.folder WHERE a.albumId = :albumId")
    Page<AssetEntity> findByAlbumId(@Param("albumId") Long albumId, Pageable pageable);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNotNull();

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt < :cutoff AND a.deletedAt IS NOT NULL")
    List<AssetEntity> findByDeletedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO asset_tags (asset_id, tag_id) VALUES (:assetId, :tagId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addTagToAsset(@Param("assetId") Long assetId, @Param("tagId") Long tagId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM asset_tags WHERE asset_id = :assetId AND tag_id = :tagId", nativeQuery = true)
    int removeTagFromAsset(@Param("assetId") Long assetId, @Param("tagId") Long tagId);

    @Query(value = "SELECT COUNT(*) > 0 FROM asset_tags WHERE asset_id = :assetId AND tag_id = :tagId", nativeQuery = true)
    boolean hasTag(@Param("assetId") Long assetId, @Param("tagId") Long tagId);

    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM AssetEntity a WHERE a.deletedAt IS NULL")
    long sumFileSize();

    @Query("SELECT COUNT(a) FROM AssetEntity a WHERE a.deletedAt IS NULL AND a.hash IS NOT NULL AND a.hash IN (SELECT a2.hash FROM AssetEntity a2 WHERE a2.deletedAt IS NULL AND a2.hash IS NOT NULL GROUP BY a2.hash HAVING COUNT(a2) > 1)")
    long countDuplicates();

    @Query("SELECT f.path as folderPath, COUNT(a) as assetCount FROM AssetEntity a JOIN a.folder f WHERE a.deletedAt IS NULL GROUP BY f.path ORDER BY COUNT(a) DESC")
    List<FolderAssetCount> findTopFoldersByAssetCount(Pageable pageable);

    @Query("SELECT a.assetId as assetId, a.fileName as fileName, f.path as folderPath, a.fileSize as fileSize FROM AssetEntity a JOIN a.folder f WHERE a.deletedAt IS NULL ORDER BY a.thumbnailCreationDateTime DESC")
    List<AssetSummary> findRecentAssets(Pageable pageable);

    @Query("SELECT f.path as folderPath, SUM(a.fileSize) as bytes FROM AssetEntity a JOIN a.folder f WHERE a.deletedAt IS NULL GROUP BY f.path ORDER BY SUM(a.fileSize) DESC")
    List<FolderStorageProjection> sumFileSizeByFolder();

    @Query("SELECT LOWER(SUBSTRING(a.fileName, LOCATE('.', a.fileName) + 1)) as extension, COUNT(a) as cnt FROM AssetEntity a WHERE a.deletedAt IS NULL AND LOCATE('.', a.fileName) > 0 GROUP BY LOWER(SUBSTRING(a.fileName, LOCATE('.', a.fileName) + 1)) ORDER BY COUNT(a) DESC")
    List<FormatProjection> countByExtension();

    @Query("SELECT FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM') as month, COUNT(a) as cnt FROM AssetEntity a WHERE a.deletedAt IS NULL AND a.fileCreationDateTime IS NOT NULL GROUP BY FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM') ORDER BY FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM') ASC")
    List<MonthlyCountProjection> countByCreationMonth();

    @Query("SELECT a.rating as rating, COUNT(a) as cnt FROM AssetEntity a WHERE a.deletedAt IS NULL GROUP BY a.rating ORDER BY a.rating ASC")
    List<RatingProjection> countByRating();

    // Row-level locking on the UPDATE guarantees exactly one caller observes affected row count 1
    // when two upload-processing stages race to be the one that flips processing_status to COMPLETED
    // (PostgreSQL serializes concurrent UPDATEs to the same row; the second re-evaluates this WHERE
    // clause against the first's already-committed change).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssetEntity a SET a.processingStatus = com.jpablodrexler.photomanager.domain.enums.ProcessingStatus.COMPLETED "
            + "WHERE a.assetId = :assetId AND a.processingStatus <> com.jpablodrexler.photomanager.domain.enums.ProcessingStatus.COMPLETED "
            + "AND a.hashCompletedAt IS NOT NULL AND a.exifCompletedAt IS NOT NULL AND a.thumbnailCompletedAt IS NOT NULL")
    int completeIfAllStagesFinished(@Param("assetId") Long assetId);

    // Targeted single-column update: the three kafka-async-upload stage processors run concurrently
    // against the same row, so each must update only its own column(s) rather than round-tripping the
    // full entity through save() (which would overwrite whichever other stage had already committed).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssetEntity a SET a.hash = :hash, a.hashCompletedAt = :hashCompletedAt WHERE a.assetId = :assetId")
    void updateHash(@Param("assetId") Long assetId, @Param("hash") String hash,
                     @Param("hashCompletedAt") LocalDateTime hashCompletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssetEntity a SET a.exifCompletedAt = :exifCompletedAt WHERE a.assetId = :assetId")
    void updateExifCompletedAt(@Param("assetId") Long assetId, @Param("exifCompletedAt") LocalDateTime exifCompletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssetEntity a SET a.thumbnailCreationDateTime = :thumbnailCreationDateTime, "
            + "a.thumbnailCompletedAt = :thumbnailCompletedAt WHERE a.assetId = :assetId")
    void updateThumbnail(@Param("assetId") Long assetId,
                         @Param("thumbnailCreationDateTime") LocalDateTime thumbnailCreationDateTime,
                         @Param("thumbnailCompletedAt") LocalDateTime thumbnailCompletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssetEntity a SET a.processingStatus = :status WHERE a.assetId = :assetId")
    void updateProcessingStatus(@Param("assetId") Long assetId, @Param("status") ProcessingStatus status);

    default Page<AssetEntity> findWithFilters(FolderEntity folder, String search, LocalDateTime dateFrom,
                                              LocalDateTime dateTo, Integer minRating, Set<String> tags,
                                              Pageable pageable) {
        Specification<AssetEntity> spec = (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("folder", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (folder != null) {
                predicates.add(cb.equal(root.get("folder"), folder));
            }
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (search != null) {
                predicates.add(cb.like(cb.lower(root.get("fileName")), search));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fileCreationDateTime"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fileCreationDateTime"), dateTo));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (tags != null && !tags.isEmpty()) {
                for (String tagName : tags) {
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<AssetEntity> subAsset = sub.from(AssetEntity.class);
                    jakarta.persistence.criteria.Join<AssetEntity, TagEntity> subTag = subAsset.join("tags");
                    sub.select(subAsset.get("assetId"))
                            .where(cb.and(
                                    cb.equal(subAsset.get("assetId"), root.get("assetId")),
                                    cb.equal(subTag.get("name"), tagName.toLowerCase())
                            ));
                    predicates.add(cb.exists(sub));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
