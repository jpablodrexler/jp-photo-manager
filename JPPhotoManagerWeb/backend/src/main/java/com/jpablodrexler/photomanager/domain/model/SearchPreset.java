package com.jpablodrexler.photomanager.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPreset {

    private Long presetId;
    private UUID userId;
    private String name;
    private String filterJson;
    private Instant createdAt;
}
