package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AlbumFilterJsonConverter.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AlbumWebMapper {

    @Mapping(source = "filterJson", target = "filterJson", qualifiedByName = "deserializeFilterJson")
    AlbumSummaryResponseDto toSummaryDto(AlbumData data);

    @Mapping(source = "summary.filterJson", target = "filterJson", qualifiedByName = "deserializeFilterJson")
    AlbumResponseDto toDto(AlbumData summary, PaginatedData<AssetResponseDto> assets);
}
