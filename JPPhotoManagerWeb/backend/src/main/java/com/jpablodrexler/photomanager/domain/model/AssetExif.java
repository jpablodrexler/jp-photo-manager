package com.jpablodrexler.photomanager.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetExif {

    private Long assetId;
    private String cameraMake;
    private String cameraModel;
    private String lensModel;
    private String exposureTime;
    private Double fNumber;
    private Integer isoSpeed;
    private Double focalLength;
    private LocalDateTime dateTaken;
    private Integer widthPixels;
    private Integer heightPixels;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private Map<String, String> rawExif;
}
