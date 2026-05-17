package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AlbumSummaryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlbumWebMapper {

    AlbumSummaryDto toSummaryDto(AlbumData data);
}
