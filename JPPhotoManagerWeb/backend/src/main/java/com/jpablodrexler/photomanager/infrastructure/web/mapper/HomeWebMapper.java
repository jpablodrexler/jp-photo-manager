package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetSummary;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.HomeStatsResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HomeWebMapper {

    HomeStatsResponseDto toDto(HomeStats stats);

    AssetSummaryResponseDto toDto(AssetSummary summary);
}
