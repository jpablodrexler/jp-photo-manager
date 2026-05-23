package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAudio {

    private Long assetId;
    private String title;
    private String artist;
    private String album;
    private Integer durationSeconds;
    private Integer bitrateKbps;
    private Integer sampleRateHz;
}
