package com.jpablodrexler.photomanager.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_exif")
@Data
public class AssetExif {

    @Id
    @Column(name = "asset_id")
    private Long assetId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "camera_make")
    private String cameraMake;

    @Column(name = "camera_model")
    private String cameraModel;

    @Column(name = "lens_model")
    private String lensModel;

    @Column(name = "exposure_time")
    private String exposureTime;

    @Column(name = "f_number")
    private Double fNumber;

    @Column(name = "iso_speed")
    private Integer isoSpeed;

    @Column(name = "focal_length")
    private Double focalLength;

    @Column(name = "date_taken")
    private LocalDateTime dateTaken;

    @Column(name = "width_pixels")
    private Integer widthPixels;

    @Column(name = "height_pixels")
    private Integer heightPixels;

    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Column(name = "gps_longitude")
    private Double gpsLongitude;
}
