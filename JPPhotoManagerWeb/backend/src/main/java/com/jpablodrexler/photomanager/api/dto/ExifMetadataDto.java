package com.jpablodrexler.photomanager.api.dto;

import com.jpablodrexler.photomanager.domain.entity.AssetExif;

import java.time.LocalDateTime;

public record ExifMetadataDto(
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
) {
    public static ExifMetadataDto from(AssetExif exif) {
        if (exif == null) return null;
        return new ExifMetadataDto(
                exif.getCameraMake(),
                exif.getCameraModel(),
                exif.getLensModel(),
                exif.getExposureTime(),
                exif.getFNumber(),
                exif.getIsoSpeed(),
                exif.getFocalLength(),
                exif.getDateTaken(),
                exif.getWidthPixels(),
                exif.getHeightPixels(),
                exif.getGpsLatitude(),
                exif.getGpsLongitude()
        );
    }
}
