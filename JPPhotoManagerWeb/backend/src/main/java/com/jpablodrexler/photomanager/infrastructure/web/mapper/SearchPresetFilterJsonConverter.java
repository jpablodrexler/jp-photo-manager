package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchPresetFilterJsonConverter {

    private final ObjectMapper objectMapper;

    @Named("extractSearch")
    public String extractSearch(String filterJson) {
        FilterPreset filter = parseFilterJson(filterJson);
        return filter != null ? filter.search() : null;
    }

    @Named("extractDateFrom")
    public String extractDateFrom(String filterJson) {
        FilterPreset filter = parseFilterJson(filterJson);
        return filter != null ? filter.dateFrom() : null;
    }

    @Named("extractDateTo")
    public String extractDateTo(String filterJson) {
        FilterPreset filter = parseFilterJson(filterJson);
        return filter != null ? filter.dateTo() : null;
    }

    @Named("extractMinRating")
    public Integer extractMinRating(String filterJson) {
        FilterPreset filter = parseFilterJson(filterJson);
        return filter != null ? filter.minRating() : null;
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
