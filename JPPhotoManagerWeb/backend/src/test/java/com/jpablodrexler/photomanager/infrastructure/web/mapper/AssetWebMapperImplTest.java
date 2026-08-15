package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.CropRegion;
import com.jpablodrexler.photomanager.domain.model.RenamePreview;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CropAssetRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.ExifMetadataResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.RenamePreviewResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.TimelineGroupResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the AssetWebMapper methods not already exercised by WebMappersTest (asset-to-DTO field
 * mapping), namely: EXIF/rename-preview DTO mapping, the crop-region request DTO, the timeline
 * group default method, and the tags default-method helper.
 */
class AssetWebMapperImplTest {

    private final AssetWebMapper sut = new AssetWebMapperImpl();

    @Test
    void toDto_assetExif_mapsAllFieldsExceptFNumber() {
        AssetExif exif = AssetExif.builder()
                .assetId(1L)
                .cameraMake("Canon")
                .cameraModel("EOS R5")
                .lensModel("RF 50mm")
                .exposureTime("1/200")
                .fNumber(2.8)
                .isoSpeed(200)
                .focalLength(50.0)
                .dateTaken(LocalDateTime.of(2026, 1, 1, 12, 0))
                .widthPixels(4000)
                .heightPixels(3000)
                .gpsLatitude(10.5)
                .gpsLongitude(-20.5)
                .rawExif(Map.of("LensModel", "RF 50mm"))
                .build();

        ExifMetadataResponseDto result = sut.toDto(exif);

        assertThat(result.cameraMake()).isEqualTo("Canon");
        assertThat(result.cameraModel()).isEqualTo("EOS R5");
        assertThat(result.lensModel()).isEqualTo("RF 50mm");
        assertThat(result.exposureTime()).isEqualTo("1/200");
        assertThat(result.isoSpeed()).isEqualTo(200);
        assertThat(result.focalLength()).isEqualTo(50.0);
        assertThat(result.dateTaken()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        assertThat(result.widthPixels()).isEqualTo(4000);
        assertThat(result.heightPixels()).isEqualTo(3000);
        assertThat(result.gpsLatitude()).isEqualTo(10.5);
        assertThat(result.gpsLongitude()).isEqualTo(-20.5);
        assertThat(result.rawExif()).isEqualTo(Map.of("LensModel", "RF 50mm"));
    }

    @Test
    void toDto_nullAssetExif_returnsNull() {
        assertThat(sut.toDto((AssetExif) null)).isNull();
    }

    @Test
    void toDto_renamePreview_mapsAllFields() {
        RenamePreview preview = new RenamePreview(3L, "old.jpg", "new.jpg");

        RenamePreviewResponseDto result = sut.toDto(preview);

        assertThat(result.assetId()).isEqualTo(3L);
        assertThat(result.oldName()).isEqualTo("old.jpg");
        assertThat(result.newName()).isEqualTo("new.jpg");
    }

    @Test
    void toDto_nullRenamePreview_returnsNull() {
        assertThat(sut.toDto((RenamePreview) null)).isNull();
    }

    @Test
    void toDomain_cropAssetRequestDto_mapsAllFields() {
        CropAssetRequestDto dto = new CropAssetRequestDto("thumbnail", 10, 20, 100, 200);

        CropRegion result = sut.toDomain(dto);

        assertThat(result.formatKey()).isEqualTo("thumbnail");
        assertThat(result.x()).isEqualTo(10);
        assertThat(result.y()).isEqualTo(20);
        assertThat(result.width()).isEqualTo(100);
        assertThat(result.height()).isEqualTo(200);
    }

    @Test
    void toDomain_nullCropAssetRequestDto_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toTimelineGroupDto_mapsGroupFieldsAndAssetsViaAssetToDto() {
        Asset asset = Asset.builder().assetId(1L).fileName("a.jpg").build();
        TimelineGroup group = new TimelineGroup(LocalDate.of(2026, 1, 1), "January 2026", List.of(asset));

        TimelineGroupResponseDto result = sut.toTimelineGroupDto(group);

        assertThat(result.localDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.label()).isEqualTo("January 2026");
        assertThat(result.assets()).hasSize(1);
        assertThat(result.assets().get(0).getAssetId()).isEqualTo(1L);
        assertThat(result.assets().get(0).getFileName()).isEqualTo("a.jpg");
    }

    @Test
    void tagsSetToList_nullTags_returnsEmptyList() {
        assertThat(sut.tagsSetToList(null)).isEmpty();
    }

    @Test
    void tagsSetToList_unsortedTags_returnsSortedList() {
        List<String> result = sut.tagsSetToList(Set.of("zebra", "apple", "mango"));

        assertThat(result).containsExactly("apple", "mango", "zebra");
    }

    @Test
    void toDto_asset_tagsAreSortedInResponseDto() {
        Asset asset = Asset.builder()
                .assetId(1L)
                .fileName("a.jpg")
                .tags(Set.of("zebra", "apple"))
                .build();

        AssetResponseDto result = sut.toDto(asset);

        assertThat(result.getTags()).containsExactly("apple", "zebra");
    }
}
