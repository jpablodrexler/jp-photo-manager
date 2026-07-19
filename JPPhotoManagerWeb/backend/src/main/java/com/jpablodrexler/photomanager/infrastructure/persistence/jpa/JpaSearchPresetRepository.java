package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SearchPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSearchPresetRepository extends JpaRepository<SearchPresetEntity, Long> {

    List<SearchPresetEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<SearchPresetEntity> findByPresetIdAndUser_Id(Long presetId, UUID userId);
}
