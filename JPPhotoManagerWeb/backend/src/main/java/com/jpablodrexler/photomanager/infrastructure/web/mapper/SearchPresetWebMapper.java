package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SearchPresetResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(componentModel = "spring")
public interface SearchPresetWebMapper {

    Logger LOG = LoggerFactory.getLogger(SearchPresetWebMapper.class);

    default SearchPresetResponseDto toDto(SearchPreset preset) {
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

    @Named("parseFilterJson")
    default FilterPreset parseFilterJson(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(filterJson, FilterPreset.class);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse filterJson: {}", filterJson, e);
            return null;
        }
    }
}
