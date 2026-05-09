package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;

public record AlbumData(
        Long albumId,
        String name,
        String description,
        Instant createdAt,
        long assetCount
) {}
