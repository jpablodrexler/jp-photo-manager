package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
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

@WebMvcTest(ConvertController.class)
@ActiveProfiles("test")
class ConvertControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    PhotoManagerFacade facade;

    // --- GET /api/convert/configuration ---

    @Test
    void getConfiguration_returns200WithDefinitions() throws Exception {
        ConvertAssetsDirectoriesDefinition def = buildDef("/src", "/dst");
        when(facade.getConvertAssetsConfiguration()).thenReturn(List.of(def));

        mockMvc.perform(get("/api/convert/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceDirectory").value("/src"))
                .andExpect(jsonPath("$[0].destinationDirectory").value("/dst"));
    }

    @Test
    void getConfiguration_emptyList_returns200WithEmptyArray() throws Exception {
        when(facade.getConvertAssetsConfiguration()).thenReturn(List.of());

        mockMvc.perform(get("/api/convert/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/convert/configuration ---

    @Test
    void setConfiguration_validBody_returns204() throws Exception {
        doNothing().when(facade).setConvertAssetsConfiguration(any());
        List<ConvertAssetsDirectoriesDefinition> defs = List.of(buildDef("/src", "/dst"));

        mockMvc.perform(put("/api/convert/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(defs)))
                .andExpect(status().isNoContent());

        verify(facade).setConvertAssetsConfiguration(any());
    }

    // --- GET /api/convert/run (SSE) ---

    @Test
    void run_initiatesAsyncConversion_returns200() throws Exception {
        ConvertAssetsResult result = new ConvertAssetsResult();
        result.setSuccess(true);
        when(facade.convertAssetsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(result)));

        MvcResult mvcResult = mockMvc.perform(get("/api/convert/run"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    // --- helper ---

    private ConvertAssetsDirectoriesDefinition buildDef(String source, String dest) {
        ConvertAssetsDirectoriesDefinition def = new ConvertAssetsDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }
}
