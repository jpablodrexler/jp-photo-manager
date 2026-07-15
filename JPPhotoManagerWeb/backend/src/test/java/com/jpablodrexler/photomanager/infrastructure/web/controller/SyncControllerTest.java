package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.GetSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SaveSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.sync.SyncAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.SyncDirectoryPairDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.SyncWebMapper;
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
    @MockitoBean
    SyncWebMapper syncWebMapper;

    // --- GET /api/sync/configuration ---

    @Test
    void getConfiguration_returns200WithDefinitions() throws Exception {
        SyncDirectoriesDefinition def = buildDef("/src", "/dst");
        SyncDirectoryPairDto dto = buildDto("/src", "/dst");
        when(getSyncConfigUseCase.execute()).thenReturn(List.of(def));
        when(syncWebMapper.toDtoList(List.of(def))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceDirectory").value("/src"))
                .andExpect(jsonPath("$[0].destinationDirectory").value("/dst"));
    }

    @Test
    void getConfiguration_emptyList_returns200WithEmptyArray() throws Exception {
        when(getSyncConfigUseCase.execute()).thenReturn(List.of());
        when(syncWebMapper.toDtoList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/sync/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/sync/configuration ---

    @Test
    void setConfiguration_validBody_returns204() throws Exception {
        doNothing().when(saveSyncConfigUseCase).execute(any());
        List<SyncDirectoryPairDto> dtos = List.of(buildDto("/src", "/dst"));
        when(syncWebMapper.toDomainList(any())).thenReturn(List.of(buildDef("/src", "/dst")));

        mockMvc.perform(put("/api/sync/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isNoContent());

        verify(saveSyncConfigUseCase).execute(any());
    }

    @Test
    void setConfiguration_blankSourceDirectory_returns400() throws Exception {
        List<SyncDirectoryPairDto> dtos = List.of(buildDto("", "/dst"));

        mockMvc.perform(put("/api/sync/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isBadRequest());

        verify(saveSyncConfigUseCase, never()).execute(any());
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

    // --- helpers ---

    private SyncDirectoriesDefinition buildDef(String source, String dest) {
        SyncDirectoriesDefinition def = new SyncDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }

    private SyncDirectoryPairDto buildDto(String source, String dest) {
        return new SyncDirectoryPairDto(null, source, dest, false, false, 0);
    }
}
