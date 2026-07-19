package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import lombok.Data;

import java.time.Instant;

@Data
public class AlbumResponseDto {
    private Long albumId;
    private String name;
    private String description;
    private Instant createdAt;
    private PaginatedData<AssetResponseDto> assets;
    private AlbumFilterJson filterJson;
}
