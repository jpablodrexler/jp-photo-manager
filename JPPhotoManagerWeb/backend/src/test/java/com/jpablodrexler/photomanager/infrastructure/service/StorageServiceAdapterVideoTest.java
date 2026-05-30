package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceAdapterVideoTest {

    StorageServiceAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new StorageServiceAdapter(new SimpleMeterRegistry());
    }

    @Test
    void isVideoFile_mp4LowerCase_returnsTrue() {
        assertThat(sut.isVideoFile("clip.mp4")).isTrue();
    }

    @Test
    void isVideoFile_movUpperCase_returnsTrue() {
        assertThat(sut.isVideoFile("video.MOV")).isTrue();
    }

    @Test
    void isVideoFile_mkvUpperCase_returnsTrue() {
        assertThat(sut.isVideoFile("rec.MKV")).isTrue();
    }

    @Test
    void isVideoFile_jpgFile_returnsFalse() {
        assertThat(sut.isVideoFile("photo.jpg")).isFalse();
    }

    @Test
    void getImageRotation_videoFilePath_returnsRotate0WithoutReadingFile() throws IOException {
        ImageRotation rotation = sut.getImageRotation("/does/not/exist/clip.mp4");
        assertThat(rotation).isEqualTo(ImageRotation.ROTATE_0);
    }

    @Test
    void getExifMetadata_videoFilePath_returnsAllNullRecord() {
        ExifMetadata exif = sut.getExifMetadata("/does/not/exist/clip.mov");
        assertThat(exif.cameraMake()).isNull();
        assertThat(exif.cameraModel()).isNull();
        assertThat(exif.lensModel()).isNull();
        assertThat(exif.exposureTime()).isNull();
        assertThat(exif.fNumber()).isNull();
        assertThat(exif.isoSpeed()).isNull();
        assertThat(exif.focalLength()).isNull();
        assertThat(exif.dateTaken()).isNull();
        assertThat(exif.widthPixels()).isNull();
        assertThat(exif.heightPixels()).isNull();
        assertThat(exif.gpsLatitude()).isNull();
        assertThat(exif.gpsLongitude()).isNull();
    }
}
