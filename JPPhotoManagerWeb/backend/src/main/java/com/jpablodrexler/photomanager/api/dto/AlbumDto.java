package com.jpablodrexler.photomanager.api.dto;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import lombok.Data;

import java.time.Instant;

@Data
public class AlbumDto {
    private Long albumId;
    private String name;
    private String description;
    private Instant createdAt;
    private PaginatedData<AssetDto> assets;
}
