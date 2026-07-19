package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerTagTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CropAssetUseCase cropAssetUseCase;
    @MockitoBean GetAssetsTimelineUseCase getAssetsTimelineUseCase;
    @MockitoBean GetAssetsUseCase getAssetsUseCase;
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
    void addTag_validRequest_returns201() throws Exception {
        doNothing().when(addTagToAssetUseCase).execute(anyLong(), anyString(), any());

        mockMvc.perform(post("/api/assets/1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"vacation\"}"))
                .andExpect(status().isCreated());

        verify(addTagToAssetUseCase).execute(eq(1L), eq("vacation"), any());
    }

    @Test
    void addTag_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/assets/1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeTag_tagAssigned_returns204() throws Exception {
        doNothing().when(removeTagFromAssetUseCase).execute(1L, "vacation", null);

        mockMvc.perform(delete("/api/assets/1/tags")
                        .param("name", "vacation"))
                .andExpect(status().isNoContent());

        verify(removeTagFromAssetUseCase).execute(1L, "vacation", null);
    }

    @Test
    void removeTag_tagNotAssigned_returns404() throws Exception {
        doThrow(new NoSuchElementException("not found"))
                .when(removeTagFromAssetUseCase).execute(anyLong(), anyString(), any());

        mockMvc.perform(delete("/api/assets/1/tags")
                        .param("name", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkAddTag_validRequest_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("assetIds", List.of(1L, 2L, 3L), "name", "to-print"));
        doNothing().when(bulkAddTagUseCase).execute(any(), anyString(), any());

        mockMvc.perform(post("/api/assets/tags/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(bulkAddTagUseCase).execute(eq(List.of(1L, 2L, 3L)), eq("to-print"), isNull());
    }

    @Test
    void bulkRemoveTag_validRequest_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("assetIds", List.of(4L, 5L), "name", "draft"));
        doNothing().when(bulkRemoveTagUseCase).execute(any(), anyString(), any());

        mockMvc.perform(delete("/api/assets/tags/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(bulkRemoveTagUseCase).execute(eq(List.of(4L, 5L)), eq("draft"), isNull());
    }
}
