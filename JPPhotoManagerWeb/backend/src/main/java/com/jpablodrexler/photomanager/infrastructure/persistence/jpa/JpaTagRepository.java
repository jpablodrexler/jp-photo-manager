package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaTagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByName(String name);

    List<TagEntity> findByNameContainingIgnoreCaseOrderByName(String q, Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM AssetEntity a JOIN a.tags t WHERE t.tagId = :tagId AND a.assetId <> :excludeAssetId")
    boolean isUsedByOtherAssets(@Param("tagId") Long tagId, @Param("excludeAssetId") Long excludeAssetId);
}
