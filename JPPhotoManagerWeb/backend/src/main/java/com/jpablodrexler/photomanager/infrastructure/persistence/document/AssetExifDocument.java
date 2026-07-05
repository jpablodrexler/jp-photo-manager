package com.jpablodrexler.photomanager.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "asset_exif")
public class AssetExifDocument {

    @Id
    private String id;

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

    /**
     * GeoJSON representation of {@link #gpsLatitude}/{@link #gpsLongitude}, populated only when
     * both coordinates are present. Backs the {@code 2dsphere} index used for future proximity
     * queries; the flat scalar fields remain the source of truth for the REST response.
     */
    private GeoJsonPoint location;
}
