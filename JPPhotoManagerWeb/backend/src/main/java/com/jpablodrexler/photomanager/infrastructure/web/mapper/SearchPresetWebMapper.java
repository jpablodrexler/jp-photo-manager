package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreatePresetRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SearchPresetResponseDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SearchPresetFilterJsonConverter.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SearchPresetWebMapper {

    FilterPreset toDomain(CreatePresetRequestDto dto);

    @Mapping(source = "filterJson", target = "search", qualifiedByName = "extractSearch")
    @Mapping(source = "filterJson", target = "dateFrom", qualifiedByName = "extractDateFrom")
    @Mapping(source = "filterJson", target = "dateTo", qualifiedByName = "extractDateTo")
    @Mapping(source = "filterJson", target = "minRating", qualifiedByName = "extractMinRating")
    SearchPresetResponseDto toDto(SearchPreset preset);
}
