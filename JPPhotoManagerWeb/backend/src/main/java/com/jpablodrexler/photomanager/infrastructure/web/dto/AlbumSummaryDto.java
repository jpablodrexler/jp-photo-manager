package com.jpablodrexler.photomanager.infrastructure.web.dto;

import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import lombok.Data;

import java.time.Instant;

@Data
public class AlbumSummaryDto {
    private Long albumId;
    private String name;
    private String description;
    private long assetCount;
    private Instant createdAt;
    private AlbumFilterJson filterJson;
}
