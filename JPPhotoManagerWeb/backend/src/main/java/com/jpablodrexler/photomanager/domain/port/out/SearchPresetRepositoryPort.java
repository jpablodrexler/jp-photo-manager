package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchPresetRepositoryPort {
    List<SearchPreset> findByUserId(UUID userId);
    Optional<SearchPreset> findByIdAndUserId(Long presetId, UUID userId);
    SearchPreset save(SearchPreset preset);
    void deleteById(Long presetId);
}
