package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetDrivesUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetInitialFolderUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetRecentTargetPathsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetSubFoldersUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.FolderResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.FolderWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    GetSubFoldersUseCase getSubFoldersUseCase;
    @MockitoBean
    GetDrivesUseCase getDrivesUseCase;
    @MockitoBean
    GetInitialFolderUseCase getInitialFolderUseCase;
    @MockitoBean
    GetRecentTargetPathsUseCase getRecentTargetPathsUseCase;
    @MockitoBean
    FolderWebMapper folderWebMapper;

    // --- GET /api/folders ---

    @Test
    void getFolders_noParentPath_returnsAllFolders() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        FolderResponseDto dto = buildFolderDto(1L, "/photos");
        when(getSubFoldersUseCase.execute(null)).thenReturn(List.of(folder));
        when(folderWebMapper.toDto(folder)).thenReturn(dto);

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folderId").value(1))
                .andExpect(jsonPath("$[0].path").value("/photos"))
                .andExpect(jsonPath("$[0].name").value("photos"));
    }

    @Test
    void getFolders_withParentPath_returnsSubFolders() throws Exception {
        Folder sub = buildFolder(2L, "/photos/2024");
        FolderResponseDto dto = buildFolderDto(2L, "/photos/2024");
        when(getSubFoldersUseCase.execute("/photos")).thenReturn(List.of(sub));
        when(folderWebMapper.toDto(sub)).thenReturn(dto);

        mockMvc.perform(get("/api/folders").param("parentPath", "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/photos/2024"));
    }

    @Test
    void getFolders_emptyResult_returns200WithEmptyArray() throws Exception {
        when(getSubFoldersUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/folders/drives ---

    @Test
    void getDrives_returns200WithDriveList() throws Exception {
        when(getDrivesUseCase.execute()).thenReturn(List.of("/", "/mnt/usb"));

        mockMvc.perform(get("/api/folders/drives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("/"))
                .andExpect(jsonPath("$[1]").value("/mnt/usb"));
    }

    // --- GET /api/folders/initial ---

    @Test
    void getInitialFolder_returns200WithConfiguredPath() throws Exception {
        when(getInitialFolderUseCase.execute()).thenReturn("/home/user/Pictures");

        mockMvc.perform(get("/api/folders/initial"))
                .andExpect(status().isOk())
                .andExpect(content().string("/home/user/Pictures"));
    }

    // --- GET /api/folders/recent-paths ---

    @Test
    void getRecentTargetPaths_returns200WithPaths() throws Exception {
        when(getRecentTargetPathsUseCase.execute()).thenReturn(List.of("/dest1", "/dest2"));

        mockMvc.perform(get("/api/folders/recent-paths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("/dest1"))
                .andExpect(jsonPath("$[1]").value("/dest2"));
    }

    @Test
    void getRecentTargetPaths_noPaths_returns200WithEmptyArray() throws Exception {
        when(getRecentTargetPathsUseCase.execute()).thenReturn(List.of());

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

    private FolderResponseDto buildFolderDto(Long id, String path) {
        FolderResponseDto dto = new FolderResponseDto();
        dto.setFolderId(id);
        dto.setPath(path);
        dto.setName(path.substring(path.lastIndexOf('/') + 1));
        return dto;
    }
}
