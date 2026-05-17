package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AlbumSummaryDto;
import org.springframework.stereotype.Component;

@Component
public class AlbumWebMapper {

    public AlbumSummaryDto toSummaryDto(AlbumData data) {
        AlbumSummaryDto dto = new AlbumSummaryDto();
        dto.setAlbumId(data.albumId());
        dto.setName(data.name());
        dto.setDescription(data.description());
        dto.setCreatedAt(data.createdAt());
        dto.setAssetCount(data.assetCount());
        return dto;
    }
}
