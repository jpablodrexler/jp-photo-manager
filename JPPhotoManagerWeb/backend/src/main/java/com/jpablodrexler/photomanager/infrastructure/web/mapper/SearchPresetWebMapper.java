package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SearchPresetResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchPresetWebMapper {

    private final ObjectMapper objectMapper;

    public SearchPresetResponseDto toDto(SearchPreset preset) {
        FilterPreset filter = parseFilterJson(preset.getFilterJson());
        return new SearchPresetResponseDto(
                preset.getPresetId(),
                preset.getName(),
                preset.getCreatedAt(),
                filter != null ? filter.search() : null,
                filter != null ? filter.dateFrom() : null,
                filter != null ? filter.dateTo() : null,
                filter != null ? filter.minRating() : null
        );
    }

    private FilterPreset parseFilterJson(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(filterJson, FilterPreset.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse filterJson: {}", filterJson, e);
            return null;
        }
    }
}
