package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.dto.DownloadAssetsRequest;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "photomanager.max-download-assets=500")
class AssetControllerDownloadTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PhotoManagerFacade facade;

    @MockitoBean
    ThumbnailStorageService thumbnailStorageService;

    @Test
    void downloadAssets_validRequest_returns200WithZipHeaders() throws Exception {
        DownloadAssetsRequest request = new DownloadAssetsRequest();
        request.setAssetIds(List.of(10L, 20L, 30L));

        doNothing().when(facade).downloadAssets(eq(List.of(10L, 20L, 30L)), any());

        mockMvc.perform(post("/api/assets/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"photos.zip\""));

        verify(facade).downloadAssets(eq(List.of(10L, 20L, 30L)), any());
    }

    @Test
    void downloadAssets_emptyAssetIds_returns400() throws Exception {
        DownloadAssetsRequest request = new DownloadAssetsRequest();
        request.setAssetIds(List.of());

        mockMvc.perform(post("/api/assets/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadAssets_exceedsMaxDownloadAssets_returns400() throws Exception {
        List<Long> ids = IntStream.rangeClosed(1, 501).mapToObj(Long::valueOf).toList();
        DownloadAssetsRequest request = new DownloadAssetsRequest();
        request.setAssetIds(ids);

        mockMvc.perform(post("/api/assets/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
