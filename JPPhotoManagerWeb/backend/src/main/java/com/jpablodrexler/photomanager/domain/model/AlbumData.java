package com.jpablodrexler.photomanager.domain.model;

import java.time.Instant;

public record AlbumData(
        Long albumId,
        String name,
        String description,
        Instant createdAt,
        long assetCount,
        String filterJson
) {}
