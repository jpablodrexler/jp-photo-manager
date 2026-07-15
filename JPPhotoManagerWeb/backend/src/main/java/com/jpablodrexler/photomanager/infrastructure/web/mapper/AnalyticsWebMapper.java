package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.model.MonthlyCountEntry;
import com.jpablodrexler.photomanager.domain.model.RatingEntry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AnalyticsResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnalyticsWebMapper {

    AnalyticsResponseDto toDto(AnalyticsData data);

    AnalyticsResponseDto.FolderStorageEntryDto toDto(FolderStorageEntry entry);

    AnalyticsResponseDto.FormatEntryDto toDto(FormatEntry entry);

    AnalyticsResponseDto.MonthlyCountEntryDto toDto(MonthlyCountEntry entry);

    AnalyticsResponseDto.RatingEntryDto toDto(RatingEntry entry);
}
