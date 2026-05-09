package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.dto.RecycleBinPurgeRequest;
import com.jpablodrexler.photomanager.api.dto.RecycleBinRestoreRequest;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    PhotoManagerFacade facade;

    @Test
    void listDeleted_defaultPage_returns200WithEmptyList() throws Exception {
        when(facade.getRecycleBin(0)).thenReturn(new PaginatedData<>(List.of(), 0, 0, 0L));

        mockMvc.perform(get("/api/recycle-bin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void restore_validBody_returns204() throws Exception {
        RecycleBinRestoreRequest body = new RecycleBinRestoreRequest(List.of(1L, 2L));
        doNothing().when(facade).restoreAssets(any());

        mockMvc.perform(post("/api/recycle-bin/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(facade).restoreAssets(List.of(1L, 2L));
    }

    @Test
    void purge_withBody_returns204AndPassesIds() throws Exception {
        RecycleBinPurgeRequest body = new RecycleBinPurgeRequest(List.of(3L));
        doNothing().when(facade).purgeRecycleBin(any());

        mockMvc.perform(delete("/api/recycle-bin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(facade).purgeRecycleBin(List.of(3L));
    }

    @Test
    void purge_noBody_returns204AndPassesNull() throws Exception {
        doNothing().when(facade).purgeRecycleBin(isNull());

        mockMvc.perform(delete("/api/recycle-bin"))
                .andExpect(status().isNoContent());

        verify(facade).purgeRecycleBin(null);
    }
}
