package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.dto.MoveAssetsRequest;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.AssetExif;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PhotoManagerFacade facade;
    @MockitoBean
    ThumbnailStorageService thumbnailStorageService;

    // --- GET /api/assets ---

    @Test
    void getAssets_validRequest_returns200WithPaginatedItems() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 1L);
        PaginatedData<Asset> page = new PaginatedData<>(List.of(asset), 0, 1, 1L);

        when(facade.getAssets(eq("/photos"), eq(0), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                .param("folderPath", "/photos")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.pageIndex").value(0));
    }

    @Test
    void getAssets_emptyFolder_returns200WithEmptyItems() throws Exception {
        PaginatedData<Asset> page = new PaginatedData<>(List.of(), 0, 0, 0L);
        when(facade.getAssets(eq("/empty"), eq(0), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                .param("folderPath", "/empty")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // --- GET /api/assets/{id}/thumbnail ---

    @Test
    void getThumbnail_thumbnailExists_returns200WithJpegBytes() throws Exception {
        when(thumbnailStorageService.loadThumbnail("42.bin")).thenReturn(new byte[] { (byte) 0xFF, (byte) 0xD8 });

        mockMvc.perform(get("/api/assets/42/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getThumbnail_thumbnailMissing_returns404() throws Exception {
        when(thumbnailStorageService.loadThumbnail("99.bin")).thenReturn(null);

        mockMvc.perform(get("/api/assets/99/thumbnail"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/assets/{id}/image ---

    @Test
    void getFullImage_jpegMagicBytes_returns200WithJpegContentType() throws Exception {
        when(facade.getAssetImage(1L))
                .thenReturn(new AssetImage(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, "photo.jpg"));

        mockMvc.perform(get("/api/assets/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getFullImage_assetNotFound_returns404() throws Exception {
        when(facade.getAssetImage(99L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/assets/99/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFullImage_pngMagicBytes_returns200WithPngContentType() throws Exception {
        byte[] pngBytes = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n' };
        when(facade.getAssetImage(2L))
                .thenReturn(new AssetImage(pngBytes, "image.png"));

        mockMvc.perform(get("/api/assets/2/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getFullImage_gifMagicBytes_returns200WithGifContentType() throws Exception {
        byte[] gifBytes = { 'G', 'I', 'F', '8', '9', 'a' };
        when(facade.getAssetImage(3L))
                .thenReturn(new AssetImage(gifBytes, "anim.gif"));

        mockMvc.perform(get("/api/assets/3/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_GIF));
    }

    @Test
    void getFullImage_unknownMagicBytes_returns415() throws Exception {
        when(facade.getAssetImage(4L))
                .thenReturn(new AssetImage(new byte[] { 1, 2, 3 }, "disguised.jpg"));

        mockMvc.perform(get("/api/assets/4/image"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getFullImage_pngMagicBytesWithJpgExtension_returns200WithPngContentType() throws Exception {
        byte[] pngBytes = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n' };
        when(facade.getAssetImage(5L))
                .thenReturn(new AssetImage(pngBytes, "misleading.jpg"));

        mockMvc.perform(get("/api/assets/5/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    // --- GET /api/assets/catalog (SSE) ---

    @Test
    void catalogAssets_initiatesAsyncProcessing_returns200() throws Exception {
        when(facade.catalogAssetsAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(get("/api/assets/catalog"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    // --- POST /api/assets/move ---

    @Test
    void moveAssets_validRequest_returns200WithTrue() throws Exception {
        when(facade.moveAssets(any(), eq("/dest"), eq(false))).thenReturn(true);

        MoveAssetsRequest request = new MoveAssetsRequest();
        request.setAssetIds(new Long[] { 1L, 2L });
        request.setDestinationFolderPath("/dest");
        request.setPreserveOriginal(false);

        mockMvc.perform(post("/api/assets/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void moveAssets_moveFails_returns200WithFalse() throws Exception {
        when(facade.moveAssets(any(), eq("/dest"), eq(false))).thenReturn(false);

        MoveAssetsRequest request = new MoveAssetsRequest();
        request.setAssetIds(new Long[] { 1L });
        request.setDestinationFolderPath("/dest");
        request.setPreserveOriginal(false);

        mockMvc.perform(post("/api/assets/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // --- DELETE /api/assets ---

    @Test
    void deleteAssets_validRequest_returns204() throws Exception {
        doNothing().when(facade).deleteAssets(any(), anyBoolean());

        mockMvc.perform(delete("/api/assets")
                .param("assetIds", "1", "2")
                .param("deleteFiles", "true"))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/assets/duplicates ---

    @Test
    void getDuplicatedAssets_duplicatesExist_returns200WithGroups() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset a1 = buildAsset(folder, "a.jpg", 1L);
        Asset a2 = buildAsset(folder, "b.jpg", 2L);
        when(facade.getDuplicatedAssets()).thenReturn(List.of(List.of(a1, a2)));

        mockMvc.perform(get("/api/assets/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isArray())
                .andExpect(jsonPath("$[0].length()").value(2));
    }

    @Test
    void getDuplicatedAssets_noDuplicates_returns200WithEmptyList() throws Exception {
        when(facade.getDuplicatedAssets()).thenReturn(List.of());

        mockMvc.perform(get("/api/assets/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/assets/{assetId}/exif ---

    @Test
    void getExifMetadata_exifExists_returns200WithFields() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 42L);
        AssetExif exif = new AssetExif();
        exif.setAsset(asset);
        exif.setCameraMake("Canon");
        exif.setCameraModel("EOS 90D");
        exif.setIsoSpeed(400);

        when(facade.getAssetExif(42L)).thenReturn(exif);

        mockMvc.perform(get("/api/assets/42/exif"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cameraMake").value("Canon"))
                .andExpect(jsonPath("$.cameraModel").value("EOS 90D"))
                .andExpect(jsonPath("$.isoSpeed").value(400));
    }

    @Test
    void getExifMetadata_noExifRow_returns204() throws Exception {
        when(facade.getAssetExif(42L)).thenReturn(null);

        mockMvc.perform(get("/api/assets/42/exif"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getExifMetadata_assetNotFound_returns404() throws Exception {
        when(facade.getAssetExif(99L)).thenThrow(new java.util.NoSuchElementException("not found"));

        mockMvc.perform(get("/api/assets/99/exif"))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }

    private Asset buildAsset(Folder folder, String fileName, Long id) {
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFolder(folder);
        asset.setFileName(fileName);
        return asset;
    }
}
