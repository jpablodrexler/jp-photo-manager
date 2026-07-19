package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Asset;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerSearchTest {

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
    ReprocessAssetUseCase reprocessAssetUseCase;
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
    GetAssetThumbnailUseCase getAssetThumbnailUseCase;
    @MockitoBean
    GetFolderIdByPathUseCase getFolderIdByPathUseCase;
    @MockitoBean
    AssetWebMapper assetWebMapper;
    @MockitoBean
    MeterRegistry meterRegistry;
    @MockitoBean
    KafkaProgressRegistry kafkaProgressRegistry;
    @MockitoBean
    GetCurrentUserUseCase getCurrentUserUseCase;

    private PaginatedResult<Asset> emptyPage() {
        return new PaginatedResult<>(List.of(), 0L, 0, 50);
    }

    @Test
    void getAssets_noFilters_callsUseCaseWithNullSearch() throws Exception {
        when(getFolderIdByPathUseCase.execute(any())).thenReturn(null);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("page", "0")
                        .param("sort", "FILE_NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        verify(getAssetsUseCase).execute(argThat(filter ->
                filter.sortCriteria() == SortCriteria.FILE_NAME && filter.search() == null));
    }

    @Test
    void getAssets_withSearchParam_callsUseCaseWithSearch() throws Exception {
        when(getFolderIdByPathUseCase.execute(any())).thenReturn(null);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("search", "vacation"))
                .andExpect(status().isOk());

        verify(getAssetsUseCase).execute(argThat(filter -> "vacation".equals(filter.search())));
    }

    @Test
    void getAssets_withDateParams_callsUseCaseWithParsedLocalDates() throws Exception {
        when(getFolderIdByPathUseCase.execute(any())).thenReturn(null);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("dateFrom", "2024-01-01")
                        .param("dateTo", "2024-12-31"))
                .andExpect(status().isOk());

        verify(getAssetsUseCase).execute(argThat(filter ->
                LocalDate.of(2024, 1, 1).equals(filter.dateFrom()) &&
                LocalDate.of(2024, 12, 31).equals(filter.dateTo())));
    }

    @Test
    void getAssets_withTagsParam_passesTagSetToUseCase() throws Exception {
        when(getFolderIdByPathUseCase.execute(any())).thenReturn(null);
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("tags", "vacation,family"))
                .andExpect(status().isOk());

        verify(getAssetsUseCase).execute(argThat(filter ->
                filter.tags() != null && filter.tags().containsAll(Set.of("vacation", "family"))));
    }
}
