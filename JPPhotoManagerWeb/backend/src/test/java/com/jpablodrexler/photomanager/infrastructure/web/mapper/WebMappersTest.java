package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.FolderResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WebMappersTest {

    private final AssetWebMapper assetWebMapper = new AssetWebMapperImpl();
    private final FolderWebMapper folderWebMapper = new FolderWebMapperImpl();
    private final AlbumWebMapper albumWebMapper = new AlbumWebMapperImpl(new AlbumFilterJsonConverter(new ObjectMapper()));

    @Test
    void assetWebMapper_toDto_mapsAllFields() {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        Asset asset = Asset.builder()
                .assetId(42L)
                .folder(folder)
                .fileName("photo.jpg")
                .fileSize(1024L)
                .pixelWidth(1920)
                .pixelHeight(1080)
                .thumbnailPixelWidth(200)
                .thumbnailPixelHeight(150)
                .imageRotation(ImageRotation.ROTATE_0)
                .thumbnailCreationDateTime(LocalDateTime.of(2024, 1, 1, 0, 0))
                .hash("abc123")
                .fileCreationDateTime(LocalDateTime.of(2024, 1, 2, 0, 0))
                .fileModificationDateTime(LocalDateTime.of(2024, 1, 3, 0, 0))
                .rating(4)
                .build();

        AssetResponseDto dto = assetWebMapper.toDto(asset);

        assertThat(dto.getAssetId()).isEqualTo(42L);
        assertThat(dto.getFolderId()).isEqualTo(1L);
        assertThat(dto.getFolderPath()).isEqualTo("/photos");
        assertThat(dto.getFileName()).isEqualTo("photo.jpg");
        assertThat(dto.getFileSize()).isEqualTo(1024L);
        assertThat(dto.getPixelWidth()).isEqualTo(1920);
        assertThat(dto.getPixelHeight()).isEqualTo(1080);
        assertThat(dto.getThumbnailPixelWidth()).isEqualTo(200);
        assertThat(dto.getThumbnailPixelHeight()).isEqualTo(150);
        assertThat(dto.getImageRotation()).isEqualTo(ImageRotation.ROTATE_0);
        assertThat(dto.getHash()).isEqualTo("abc123");
        assertThat(dto.getRating()).isEqualTo(4);
        long expectedVersion = LocalDateTime.of(2024, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC);
        assertThat(dto.getThumbnailUrl()).isEqualTo("/api/assets/42/thumbnail?v=" + expectedVersion);
        assertThat(dto.getImageUrl()).isEqualTo("/api/assets/42/image");
    }

    @Test
    void assetWebMapper_toDto_nullFolder_doesNotSetFolderFields() {
        Asset asset = Asset.builder().assetId(1L).fileName("a.jpg").build();

        AssetResponseDto dto = assetWebMapper.toDto(asset);

        assertThat(dto.getFolderId()).isNull();
        assertThat(dto.getFolderPath()).isNull();
    }

    @Test
    void assetWebMapper_toDto_noThumbnailCreationDateTime_omitsVersionQueryParam() {
        Asset asset = Asset.builder().assetId(1L).fileName("a.jpg").build();

        AssetResponseDto dto = assetWebMapper.toDto(asset);

        assertThat(dto.getThumbnailUrl()).isEqualTo("/api/assets/1/thumbnail");
    }

    @Test
    void folderWebMapper_toDto_mapsAllFields() {
        Folder folder = Folder.builder().folderId(5L).path("/photos/2024").build();

        FolderResponseDto dto = folderWebMapper.toDto(folder);

        assertThat(dto.getFolderId()).isEqualTo(5L);
        assertThat(dto.getPath()).isEqualTo("/photos/2024");
        assertThat(dto.getName()).isEqualTo("2024");
        assertThat(dto.getParentPath()).isEqualTo("/photos");
    }

    @Test
    void albumWebMapper_toSummaryDto_mapsAllFields() {
        Instant now = Instant.now();
        AlbumData data = new AlbumData(10L, "Vacation", "Summer 2024", now, 42L, null);

        AlbumSummaryResponseDto dto = albumWebMapper.toSummaryDto(data);

        assertThat(dto.getAlbumId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Vacation");
        assertThat(dto.getDescription()).isEqualTo("Summer 2024");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getAssetCount()).isEqualTo(42L);
    }
}
