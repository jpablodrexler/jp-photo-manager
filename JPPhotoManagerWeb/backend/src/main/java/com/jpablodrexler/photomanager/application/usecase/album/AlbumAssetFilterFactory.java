package com.jpablodrexler.photomanager.application.usecase.album;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
class AlbumAssetFilterFactory {

    private final ObjectMapper objectMapper;

    AssetFilter build(String filterJsonStr, int page, int pageSize) {
        try {
            AlbumFilterJson f = objectMapper.readValue(filterJsonStr, AlbumFilterJson.class);
            LocalDate dateFrom = f.dateFrom() != null ? LocalDate.parse(f.dateFrom()) : null;
            LocalDate dateTo = f.dateTo() != null ? LocalDate.parse(f.dateTo()) : null;
            return new AssetFilter(null, f.search(), dateFrom, dateTo, f.minRating(), SortCriteria.FILE_NAME, page, pageSize, false, Set.of());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse filterJson", e);
        }
    }
}
