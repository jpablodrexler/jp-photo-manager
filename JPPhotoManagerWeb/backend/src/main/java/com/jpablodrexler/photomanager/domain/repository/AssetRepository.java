package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
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
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

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

    default Page<Asset> findByFolderWithFilters(Folder folder, String search, LocalDateTime dateFrom,
                                                LocalDateTime dateTo, Integer minRating, Pageable pageable) {
        Specification<Asset> spec = (root, query, cb) -> {
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
