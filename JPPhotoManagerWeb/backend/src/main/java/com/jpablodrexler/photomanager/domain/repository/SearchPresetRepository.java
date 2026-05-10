package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.SearchPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchPresetRepository extends JpaRepository<SearchPreset, Long> {

    List<SearchPreset> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<SearchPreset> findByPresetIdAndUser_Id(Long presetId, UUID userId);
}
