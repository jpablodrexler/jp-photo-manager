package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.port.out.SearchPresetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaSearchPresetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.SearchPresetEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchPresetRepositoryImpl implements SearchPresetRepository {

    private final JpaSearchPresetRepository jpa;
    private final SearchPresetEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SearchPreset> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpa.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SearchPreset> findByIdAndUserId(Long presetId, UUID userId) {
        return jpa.findByPresetIdAndUser_Id(presetId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public SearchPreset save(SearchPreset preset) {
        throw new UnsupportedOperationException("SearchPreset save not yet implemented");
    }

    @Override
    @Transactional
    public void deleteById(Long presetId) {
        jpa.deleteById(presetId);
    }
}
