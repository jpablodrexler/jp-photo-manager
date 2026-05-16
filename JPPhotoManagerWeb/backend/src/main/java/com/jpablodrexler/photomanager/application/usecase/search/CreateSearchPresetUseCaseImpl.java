package com.jpablodrexler.photomanager.application.usecase.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.port.in.search.CreateSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.SearchPresetRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSearchPresetUseCaseImpl implements CreateSearchPresetUseCase {

    private final SearchPresetRepository searchPresetRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SearchPreset execute(UUID userId, String name, FilterPreset criteria) {
        userRepository.findById(userId).orElseThrow();
        String filterJson;
        try {
            filterJson = objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize filter preset", e);
        }
        SearchPreset preset = new SearchPreset();
        preset.setUserId(userId);
        preset.setName(name);
        preset.setFilterJson(filterJson);
        preset.setCreatedAt(Instant.now());
        return searchPresetRepository.save(preset);
    }
}
