package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAssetRepository extends JpaRepository<AssetEntity, Long>, JpaSpecificationExecutor<AssetEntity> {

    List<AssetEntity> findByFolder(FolderEntity folder);

    Optional<AssetEntity> findByFolderAndFileName(FolderEntity folder, String fileName);

    boolean existsByFolderAndFileName(FolderEntity folder, String fileName);

    void deleteByFolderAndFileName(FolderEntity folder, String fileName);

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt IS NOT NULL ORDER BY a.deletedAt DESC")
    Page<AssetEntity> findDeletedOrderByDeletedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt IS NULL")
    List<AssetEntity> findNotDeleted();

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.hash = :hash AND a.deletedAt IS NULL")
    List<AssetEntity> findByHashNotDeleted(String hash);

    @Query("SELECT a.hash FROM AssetEntity a GROUP BY a.hash HAVING COUNT(a) > 1")
    List<String> findDuplicateHashes();

    @Query("SELECT aa FROM AlbumEntity a JOIN a.assets aa JOIN FETCH aa.folder WHERE a.albumId = :albumId")
    Page<AssetEntity> findByAlbumId(@Param("albumId") Long albumId, Pageable pageable);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNotNull();

    @Query("SELECT a FROM AssetEntity a JOIN FETCH a.folder WHERE a.deletedAt < :cutoff AND a.deletedAt IS NOT NULL")
    List<AssetEntity> findByDeletedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    default Page<AssetEntity> findWithFilters(FolderEntity folder, String search, LocalDateTime dateFrom,
                                              LocalDateTime dateTo, Integer minRating, Pageable pageable) {
        Specification<AssetEntity> spec = (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("folder", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("folder"), folder));
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
