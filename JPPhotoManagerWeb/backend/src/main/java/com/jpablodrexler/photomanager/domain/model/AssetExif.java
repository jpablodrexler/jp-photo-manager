package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Double focalLength;
    private Integer isoSpeed;
    private LocalDateTime dateTaken;
    private Integer widthPixels;
    private Integer heightPixels;
    private Double gpsLatitude;
    private Double gpsLongitude;
}
