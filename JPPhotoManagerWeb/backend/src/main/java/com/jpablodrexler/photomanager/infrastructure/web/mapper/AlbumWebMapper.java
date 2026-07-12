package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AlbumWebMapper {

    @Mapping(source = "filterJson", target = "filterJson", qualifiedByName = "deserializeFilterJson")
    AlbumSummaryResponseDto toSummaryDto(AlbumData data);

    @Mapping(source = "summary.filterJson", target = "filterJson", qualifiedByName = "deserializeFilterJson")
    AlbumResponseDto toDto(AlbumData summary, PaginatedData<AssetResponseDto> assets);

    @Named("deserializeFilterJson")
    default AlbumFilterJson deserializeFilterJson(String filterJson) {
        if (filterJson == null) return null;
        try {
            return new ObjectMapper().readValue(filterJson, AlbumFilterJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize filterJson", e);
        }
    }
}
