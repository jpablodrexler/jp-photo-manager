package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerUploadTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PhotoManagerFacade facade;
    @MockitoBean
    ThumbnailStorageService thumbnailStorageService;

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

        when(facade.uploadAsset(eq("/photos"), any())).thenReturn(asset);

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
        when(facade.uploadAsset(eq("/unknown"), any()))
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
        when(facade.uploadAsset(eq("/photos"), any()))
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
