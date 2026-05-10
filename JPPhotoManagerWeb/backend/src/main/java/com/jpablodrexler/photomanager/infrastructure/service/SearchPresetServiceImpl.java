package com.jpablodrexler.photomanager.infrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.domain.entity.SearchPreset;
import com.jpablodrexler.photomanager.domain.repository.SearchPresetRepository;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.SearchPresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchPresetServiceImpl implements SearchPresetService {

    private final SearchPresetRepository searchPresetRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SearchPreset> listPresets(UUID userId) {
        return searchPresetRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public SearchPreset createPreset(UUID userId, String name, FilterPreset filter) {
        var user = userRepository.findById(userId).orElseThrow();
        String filterJson;
        try {
            filterJson = objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize filter preset", e);
        }
        SearchPreset preset = new SearchPreset();
        preset.setUser(user);
        preset.setName(name);
        preset.setFilterJson(filterJson);
        preset.setCreatedAt(Instant.now());
        return searchPresetRepository.save(preset);
    }

    @Override
    @Transactional
    public void deletePreset(UUID userId, Long presetId) {
        SearchPreset preset = searchPresetRepository.findByPresetIdAndUser_Id(presetId, userId)
                .orElseThrow(() -> new SearchPresetNotFoundException(presetId));
        searchPresetRepository.delete(preset);
    }
}
