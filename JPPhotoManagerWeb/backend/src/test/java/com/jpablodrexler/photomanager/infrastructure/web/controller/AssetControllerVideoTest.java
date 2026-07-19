package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.CropAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DownloadAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetThumbnailUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsTimelineUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.MoveAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RenameAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.ReprocessAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.GetDuplicatedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetFolderIdByPathUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerVideoTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CropAssetUseCase cropAssetUseCase;
    @MockitoBean GetAssetsUseCase getAssetsUseCase;
    @MockitoBean GetAssetsTimelineUseCase getAssetsTimelineUseCase;
    @MockitoBean GetAssetImageUseCase getAssetImageUseCase;
    @MockitoBean GetAssetExifUseCase getAssetExifUseCase;
    @MockitoBean DownloadAssetsUseCase downloadAssetsUseCase;
    @MockitoBean RateAssetUseCase rateAssetUseCase;
    @MockitoBean MoveAssetsUseCase moveAssetsUseCase;
    @MockitoBean RenameAssetsUseCase renameAssetsUseCase;
    @MockitoBean UploadAssetUseCase uploadAssetUseCase;
    @MockitoBean ReprocessAssetUseCase reprocessAssetUseCase;
    @MockitoBean DeleteAssetsUseCase deleteAssetsUseCase;
    @MockitoBean CatalogAssetsUseCase catalogAssetsUseCase;
    @MockitoBean GetDuplicatedAssetsUseCase getDuplicatedAssetsUseCase;
    @MockitoBean AddTagToAssetUseCase addTagToAssetUseCase;
    @MockitoBean RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    @MockitoBean BulkAddTagUseCase bulkAddTagUseCase;
    @MockitoBean BulkRemoveTagUseCase bulkRemoveTagUseCase;
    @MockitoBean GetAssetThumbnailUseCase getAssetThumbnailUseCase;
    @MockitoBean GetFolderIdByPathUseCase getFolderIdByPathUseCase;
    @MockitoBean AssetWebMapper assetWebMapper;
    @MockitoBean MeterRegistry meterRegistry;
    @MockitoBean KafkaProgressRegistry kafkaProgressRegistry;
    @MockitoBean GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void getAssets_videoAsset_responseContainsIsVideoTrue() throws Exception {
        Folder folder = buildFolder(1L, "/videos");
        Asset videoAsset = buildAsset(folder, "clip.mp4", 1L, true);
        PaginatedResult<Asset> page = new PaginatedResult<>(List.of(videoAsset), 1L, 0, 50);

        when(getFolderIdByPathUseCase.execute("/videos")).thenReturn(1L);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(page);
        when(assetWebMapper.toDto(videoAsset)).thenReturn(buildAssetDto("clip.mp4", 1L, true));

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/videos")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isVideo").value(true));
    }

    @Test
    void getAssets_imageAsset_responseContainsIsVideoFalse() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset imageAsset = buildAsset(folder, "photo.jpg", 2L, false);
        PaginatedResult<Asset> page = new PaginatedResult<>(List.of(imageAsset), 1L, 0, 50);

        when(getFolderIdByPathUseCase.execute("/photos")).thenReturn(1L);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(page);
        when(assetWebMapper.toDto(imageAsset)).thenReturn(buildAssetDto("photo.jpg", 2L, false));

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isVideo").value(false));
    }

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }

    private Asset buildAsset(Folder folder, String fileName, Long id, boolean isVideo) {
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setVideo(isVideo);
        return asset;
    }

    private AssetResponseDto buildAssetDto(String fileName, Long id, boolean isVideo) {
        AssetResponseDto dto = new AssetResponseDto();
        dto.setAssetId(id);
        dto.setFileName(fileName);
        dto.setVideo(isVideo);
        return dto;
    }
}
