package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.convert.ConvertAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.GetConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.SaveConvertConfigUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.ConvertDirectoryPairDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.ConvertWebMapper;
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

@WebMvcTest(ConvertController.class)
@ActiveProfiles("test")
class ConvertControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GetConvertConfigUseCase getConvertConfigUseCase;
    @MockitoBean
    SaveConvertConfigUseCase saveConvertConfigUseCase;
    @MockitoBean
    ConvertAssetsUseCase convertAssetsUseCase;
    @MockitoBean
    KafkaProgressRegistry kafkaProgressRegistry;
    @MockitoBean
    ConvertWebMapper convertWebMapper;

    // --- GET /api/convert/configuration ---

    @Test
    void getConfiguration_returns200WithDefinitions() throws Exception {
        ConvertDirectoriesDefinition def = buildDef("/src", "/dst");
        ConvertDirectoryPairDto dto = buildDto("/src", "/dst");
        when(getConvertConfigUseCase.execute()).thenReturn(List.of(def));
        when(convertWebMapper.toDtoList(List.of(def))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/convert/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceDirectory").value("/src"))
                .andExpect(jsonPath("$[0].destinationDirectory").value("/dst"));
    }

    @Test
    void getConfiguration_emptyList_returns200WithEmptyArray() throws Exception {
        when(getConvertConfigUseCase.execute()).thenReturn(List.of());
        when(convertWebMapper.toDtoList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/convert/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/convert/configuration ---

    @Test
    void setConfiguration_validBody_returns204() throws Exception {
        doNothing().when(saveConvertConfigUseCase).execute(any());
        List<ConvertDirectoryPairDto> dtos = List.of(buildDto("/src", "/dst"));
        when(convertWebMapper.toDomainList(any())).thenReturn(List.of(buildDef("/src", "/dst")));

        mockMvc.perform(put("/api/convert/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isNoContent());

        verify(saveConvertConfigUseCase).execute(any());
    }

    @Test
    void setConfiguration_blankSourceDirectory_returns400() throws Exception {
        List<ConvertDirectoryPairDto> dtos = List.of(buildDto("", "/dst"));

        mockMvc.perform(put("/api/convert/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isBadRequest());

        verify(saveConvertConfigUseCase, never()).execute(any());
    }

    // --- GET /api/convert/run (SSE) ---

    @Test
    void run_initiatesAsyncConversion_returns200() throws Exception {
        doAnswer((InvocationOnMock inv) -> { inv.<SseEmitter>getArgument(1).complete(); return null; })
                .when(kafkaProgressRegistry).registerEmitter(anyLong(), any(SseEmitter.class));

        MvcResult mvcResult = mockMvc.perform(get("/api/convert/run"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    // --- helpers ---

    private ConvertDirectoriesDefinition buildDef(String source, String dest) {
        ConvertDirectoriesDefinition def = new ConvertDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }

    private ConvertDirectoryPairDto buildDto(String source, String dest) {
        return new ConvertDirectoryPairDto(null, source, dest, false, false, 0);
    }
}
