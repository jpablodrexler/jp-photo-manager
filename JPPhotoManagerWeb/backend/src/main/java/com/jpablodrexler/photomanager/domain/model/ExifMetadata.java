package com.jpablodrexler.photomanager.domain.model;

import java.time.LocalDateTime;

public record ExifMetadata(
        String cameraMake,
        String cameraModel,
        String lensModel,
        String exposureTime,
        Double fNumber,
        Integer isoSpeed,
        Double focalLength,
        LocalDateTime dateTaken,
        Integer widthPixels,
        Integer heightPixels,
        Double gpsLatitude,
        Double gpsLongitude
) {}
