package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FolderController.class)
@ActiveProfiles("test")
class FolderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    PhotoManagerFacade facade;

    // --- GET /api/folders ---

    @Test
    void getFolders_noParentPath_returnsAllFolders() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(facade.getSubFolders(null)).thenReturn(List.of(folder));

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folderId").value(1))
                .andExpect(jsonPath("$[0].path").value("/photos"))
                .andExpect(jsonPath("$[0].name").value("photos"));
    }

    @Test
    void getFolders_withParentPath_returnsSubFolders() throws Exception {
        Folder sub = buildFolder(2L, "/photos/2024");
        when(facade.getSubFolders("/photos")).thenReturn(List.of(sub));

        mockMvc.perform(get("/api/folders").param("parentPath", "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/photos/2024"));
    }

    @Test
    void getFolders_emptyResult_returns200WithEmptyArray() throws Exception {
        when(facade.getSubFolders(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/folders/drives ---

    @Test
    void getDrives_returns200WithDriveList() throws Exception {
        when(facade.getDrives()).thenReturn(List.of("/", "/mnt/usb"));

        mockMvc.perform(get("/api/folders/drives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("/"))
                .andExpect(jsonPath("$[1]").value("/mnt/usb"));
    }

    // --- GET /api/folders/initial ---

    @Test
    void getInitialFolder_returns200WithConfiguredPath() throws Exception {
        when(facade.getInitialFolder()).thenReturn("/home/user/Pictures");

        mockMvc.perform(get("/api/folders/initial"))
                .andExpect(status().isOk())
                .andExpect(content().string("/home/user/Pictures"));
    }

    // --- GET /api/folders/recent-paths ---

    @Test
    void getRecentTargetPaths_returns200WithPaths() throws Exception {
        when(facade.getRecentTargetPaths()).thenReturn(List.of("/dest1", "/dest2"));

        mockMvc.perform(get("/api/folders/recent-paths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("/dest1"))
                .andExpect(jsonPath("$[1]").value("/dest2"));
    }

    @Test
    void getRecentTargetPaths_noPaths_returns200WithEmptyArray() throws Exception {
        when(facade.getRecentTargetPaths()).thenReturn(List.of());

        mockMvc.perform(get("/api/folders/recent-paths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- helper ---

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }
}
