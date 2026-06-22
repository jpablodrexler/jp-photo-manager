package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.GetSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SaveSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(SyncController.class)
@ActiveProfiles("test")
class SyncControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GetSyncConfigUseCase getSyncConfigUseCase;
    @MockitoBean
    SaveSyncConfigUseCase saveSyncConfigUseCase;
    @MockitoBean
    SyncAssetsUseCase syncAssetsUseCase;
    @MockitoBean
    KafkaProgressRegistry kafkaProgressRegistry;

    // --- GET /api/sync/configuration ---

    @Test
    void getConfiguration_returns200WithDefinitions() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dst");
        when(getSyncConfigUseCase.execute()).thenReturn(List.of(def));

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceDirectory").value("/src"))
                .andExpect(jsonPath("$[0].destinationDirectory").value("/dst"));
    }

    @Test
    void getConfiguration_emptyList_returns200WithEmptyArray() throws Exception {
        when(getSyncConfigUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/sync/configuration ---

    @Test
    void setConfiguration_validBody_returns204() throws Exception {
        doNothing().when(saveSyncConfigUseCase).execute(any());
        List<SyncDirectoriesDefinition> defs = List.of(buildDef("/src", "/dst"));

        mockMvc.perform(put("/api/sync/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defs)))
                .andExpect(status().isNoContent());

        verify(saveSyncConfigUseCase).execute(any());
    }

    // --- GET /api/sync/run (SSE) ---

    @Test
    void run_initiatesAsyncSync_returns200() throws Exception {
        doAnswer((InvocationOnMock inv) -> { inv.<SseEmitter>getArgument(1).complete(); return null; })
                .when(kafkaProgressRegistry).registerEmitter(anyLong(), any(SseEmitter.class));

        MvcResult mvcResult = mockMvc.perform(get("/api/sync/run"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    // --- helper ---

    private SyncDirectoriesDefinition buildDef(String source, String dest) {
        SyncDirectoriesDefinition def = new SyncDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }
}
