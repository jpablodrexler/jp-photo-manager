package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.CropAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DownloadAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsTimelineUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.MoveAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RenameAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.GetDuplicatedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerUploadTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CropAssetUseCase cropAssetUseCase;
    @MockitoBean
    GetAssetsTimelineUseCase getAssetsTimelineUseCase;
    @MockitoBean
    GetAssetsUseCase getAssetsUseCase;
    @MockitoBean
    GetAssetImageUseCase getAssetImageUseCase;
    @MockitoBean
    GetAssetExifUseCase getAssetExifUseCase;
    @MockitoBean
    DownloadAssetsUseCase downloadAssetsUseCase;
    @MockitoBean
    RateAssetUseCase rateAssetUseCase;
    @MockitoBean
    MoveAssetsUseCase moveAssetsUseCase;
    @MockitoBean
    RenameAssetsUseCase renameAssetsUseCase;
    @MockitoBean
    UploadAssetUseCase uploadAssetUseCase;
    @MockitoBean
    DeleteAssetsUseCase deleteAssetsUseCase;
    @MockitoBean
    CatalogAssetsUseCase catalogAssetsUseCase;
    @MockitoBean
    GetDuplicatedAssetsUseCase getDuplicatedAssetsUseCase;
    @MockitoBean
    AddTagToAssetUseCase addTagToAssetUseCase;
    @MockitoBean
    RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    @MockitoBean
    BulkAddTagUseCase bulkAddTagUseCase;
    @MockitoBean
    BulkRemoveTagUseCase bulkRemoveTagUseCase;
    @MockitoBean
    ThumbnailPort thumbnailPort;
    @MockitoBean
    FolderRepository folderRepository;
    @MockitoBean
    AssetWebMapper assetWebMapper;
    @MockitoBean
    MeterRegistry meterRegistry;
    @MockitoBean
    KafkaProgressRegistry kafkaProgressRegistry;

    @Test
    void uploadAsset_validJpeg_returns201WithAssetDto() throws Exception {
        Folder folder = new Folder();
        folder.setFolderId(1L);
        folder.setPath("/photos");

        Asset asset = new Asset();
        asset.setAssetId(42L);
        asset.setFolder(folder);
        asset.setFileName("photo.jpg");
        asset.setFileSize(1024L);

        AssetDto dto = new AssetDto();
        dto.setAssetId(42L);
        dto.setFileName("photo.jpg");

        when(uploadAssetUseCase.execute(eq("/photos"), eq("photo.jpg"), any())).thenReturn(asset);
        when(assetWebMapper.toDto(asset)).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE, "/photos".getBytes());

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file)
                        .file(folderPathPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").value(42))
                .andExpect(jsonPath("$.fileName").value("photo.jpg"));
    }

    @Test
    void uploadAsset_nonImageContentType_returns415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "hello".getBytes());
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE, "/photos".getBytes());

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file)
                        .file(folderPathPart))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadAsset_spoofedContentTypeWithExeExtension_returns415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE, "/photos".getBytes());

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file)
                        .file(folderPathPart))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadAsset_unknownFolder_returns404() throws Exception {
        when(uploadAssetUseCase.execute(eq("/unknown"), anyString(), any()))
                .thenThrow(new FolderNotFoundException("/unknown"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE, "/unknown".getBytes());

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file)
                        .file(folderPathPart))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadAsset_ioException_returns500() throws Exception {
        when(uploadAssetUseCase.execute(eq("/photos"), anyString(), any()))
                .thenThrow(new IOException("disk full"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE, "/photos".getBytes());

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file)
                        .file(folderPathPart))
                .andExpect(status().isInternalServerError());
    }
}
