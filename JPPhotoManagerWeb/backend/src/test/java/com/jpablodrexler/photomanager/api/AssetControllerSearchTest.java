package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    PhotoManagerFacade facade;

    @MockitoBean
    ThumbnailStorageService thumbnailStorageService;

    private PaginatedData<Asset> emptyPage() {
        return new PaginatedData<>(List.of(), 0, 0, 0);
    }

    @Test
    void getAssets_noFilters_callsFacadeWithNullFilterArgs() throws Exception {
        when(facade.getAssets(any(), any(int.class), any(SortCriteria.class), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("page", "0")
                        .param("sort", "FILE_NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        verify(facade).getAssets(eq("/photos"), eq(0), eq(SortCriteria.FILE_NAME), isNull(), isNull(), isNull());
    }

    @Test
    void getAssets_withSearchParam_callsFacadeWithSearchAndNullDates() throws Exception {
        when(facade.getAssets(any(), any(int.class), any(SortCriteria.class), any(), isNull(), isNull()))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("search", "vacation"))
                .andExpect(status().isOk());

        verify(facade).getAssets(eq("/photos"), eq(0), eq(SortCriteria.FILE_NAME),
                eq("vacation"), isNull(), isNull());
    }

    @Test
    void getAssets_withDateParams_callsFacadeWithParsedLocalDates() throws Exception {
        when(facade.getAssets(any(), any(int.class), any(SortCriteria.class), isNull(), any(), any()))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("dateFrom", "2024-01-01")
                        .param("dateTo", "2024-12-31"))
                .andExpect(status().isOk());

        verify(facade).getAssets(eq("/photos"), eq(0), eq(SortCriteria.FILE_NAME),
                isNull(),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 12, 31)));
    }
}
