package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncController.class)
@ActiveProfiles("test")
class SyncControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    PhotoManagerFacade facade;

    // --- GET /api/sync/configuration ---

    @Test
    void getConfiguration_returns200WithDefinitions() throws Exception {
        SyncAssetsDirectoriesDefinition def = buildDef("/src", "/dst");
        when(facade.getSyncAssetsConfiguration()).thenReturn(List.of(def));

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceDirectory").value("/src"))
                .andExpect(jsonPath("$[0].destinationDirectory").value("/dst"));
    }

    @Test
    void getConfiguration_emptyList_returns200WithEmptyArray() throws Exception {
        when(facade.getSyncAssetsConfiguration()).thenReturn(List.of());

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/sync/configuration ---

    @Test
    void setConfiguration_validBody_returns204() throws Exception {
        doNothing().when(facade).setSyncAssetsConfiguration(any());
        List<SyncAssetsDirectoriesDefinition> defs = List.of(buildDef("/src", "/dst"));

        mockMvc.perform(put("/api/sync/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(defs)))
                .andExpect(status().isNoContent());

        verify(facade).setSyncAssetsConfiguration(any());
    }

    // --- GET /api/sync/run (SSE) ---

    @Test
    void run_initiatesAsyncSync_returns200() throws Exception {
        SyncAssetsResult result = new SyncAssetsResult();
        result.setSuccess(true);
        when(facade.syncAssetsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(result)));

        MvcResult mvcResult = mockMvc.perform(get("/api/sync/run"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    // --- helper ---

    private SyncAssetsDirectoriesDefinition buildDef(String source, String dest) {
        SyncAssetsDirectoriesDefinition def = new SyncAssetsDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }
}
