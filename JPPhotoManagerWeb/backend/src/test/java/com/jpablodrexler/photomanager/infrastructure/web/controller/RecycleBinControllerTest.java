package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.recycle.GetDeletedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.recycle.PurgeAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.recycle.RestoreAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RecycleBinPurgeRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RecycleBinRestoreRequest;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecycleBinController.class)
@ActiveProfiles("test")
class RecycleBinControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GetDeletedAssetsUseCase getDeletedAssetsUseCase;
    @MockitoBean
    RestoreAssetsUseCase restoreAssetsUseCase;
    @MockitoBean
    PurgeAssetsUseCase purgeAssetsUseCase;
    @MockitoBean
    AssetWebMapper assetWebMapper;

    // --- GET /api/recycle-bin ---

    @Test
    void listDeleted_returns200WithPaginatedAssets() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "deleted.jpg", 10L);
        PaginatedResult<Asset> result = new PaginatedResult<>(List.of(asset), 1L, 0, 50);

        AssetDto dto = new AssetDto();
        dto.setAssetId(10L);
        dto.setFileName("deleted.jpg");

        when(getDeletedAssetsUseCase.execute(0)).thenReturn(result);
        when(assetWebMapper.toDto(asset)).thenReturn(dto);

        mockMvc.perform(get("/api/recycle-bin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assetId").value(10))
                .andExpect(jsonPath("$.items[0].fileName").value("deleted.jpg"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void listDeleted_withPageParam_passesPageToUseCase() throws Exception {
        PaginatedResult<Asset> result = new PaginatedResult<>(List.of(), 0L, 2, 50);
        when(getDeletedAssetsUseCase.execute(2)).thenReturn(result);

        mockMvc.perform(get("/api/recycle-bin").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(getDeletedAssetsUseCase).execute(2);
    }

    // --- POST /api/recycle-bin/restore ---

    @Test
    void restore_validRequest_returns204() throws Exception {
        doNothing().when(restoreAssetsUseCase).execute(anyList());

        RecycleBinRestoreRequest request = new RecycleBinRestoreRequest(List.of(10L, 11L));
        mockMvc.perform(post("/api/recycle-bin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(restoreAssetsUseCase).execute(List.of(10L, 11L));
    }

    // --- DELETE /api/recycle-bin ---

    @Test
    void purge_withAssetIds_returns204() throws Exception {
        doNothing().when(purgeAssetsUseCase).execute(anyList());

        RecycleBinPurgeRequest request = new RecycleBinPurgeRequest(List.of(10L, 11L));
        mockMvc.perform(delete("/api/recycle-bin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(purgeAssetsUseCase).execute(List.of(10L, 11L));
    }

    @Test
    void purge_withoutBody_returns400() throws Exception {
        mockMvc.perform(delete("/api/recycle-bin"))
                .andExpect(status().isBadRequest());

        verify(purgeAssetsUseCase, never()).execute(any());
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
        asset.setFileSize(1024L);
        asset.setHash("abc123");
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        return asset;
    }
}
