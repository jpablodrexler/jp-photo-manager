package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetExifDocumentMapperTest {

    private final AssetExifDocumentMapper sut = new AssetExifDocumentMapperImpl();

    @Test
    void toDocument_bothCoordinatesPresent_derivesLocation() {
        AssetExif exif = AssetExif.builder()
                .assetId(1L)
                .cameraMake("Canon")
                .gpsLatitude(-34.6)
                .gpsLongitude(-58.4)
                .build();

        AssetExifDocument result = sut.toDocument(exif);

        assertThat(result.getAssetId()).isEqualTo(1L);
        assertThat(result.getCameraMake()).isEqualTo("Canon");
        assertThat(result.getLocation()).isNotNull();
        assertThat(result.getLocation().getX()).isEqualTo(-58.4);
        assertThat(result.getLocation().getY()).isEqualTo(-34.6);
    }

    @Test
    void toDocument_latitudeAbsent_locationIsNull() {
        AssetExif exif = AssetExif.builder()
                .assetId(2L)
                .gpsLongitude(-58.4)
                .build();

        AssetExifDocument result = sut.toDocument(exif);

        assertThat(result.getLocation()).isNull();
    }

    @Test
    void toDocument_bothCoordinatesAbsent_locationIsNull() {
        AssetExif exif = AssetExif.builder().assetId(3L).build();

        AssetExifDocument result = sut.toDocument(exif);

        assertThat(result.getLocation()).isNull();
    }

    @Test
    void toDomain_mapsScalarFields() {
        AssetExifDocument document = new AssetExifDocument();
        document.setAssetId(4L);
        document.setCameraMake("Nikon");
        document.setGpsLatitude(10.0);
        document.setGpsLongitude(20.0);

        AssetExif result = sut.toDomain(document);

        assertThat(result.getAssetId()).isEqualTo(4L);
        assertThat(result.getCameraMake()).isEqualTo("Nikon");
        assertThat(result.getGpsLatitude()).isEqualTo(10.0);
        assertThat(result.getGpsLongitude()).isEqualTo(20.0);
    }
}
