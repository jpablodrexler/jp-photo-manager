package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    private Long albumId;
    private UUID userId;
    private String name;
    private String description;
    private Instant createdAt;
    private List<Long> assetIds;
}
