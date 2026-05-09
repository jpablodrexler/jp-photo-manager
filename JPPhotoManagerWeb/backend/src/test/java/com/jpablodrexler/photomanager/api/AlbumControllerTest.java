package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.dto.CreateAlbumRequest;
import com.jpablodrexler.photomanager.api.dto.UpdateAlbumRequest;
import com.jpablodrexler.photomanager.api.dto.AlbumAssetIdsRequest;
import com.jpablodrexler.photomanager.api.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlbumController.class)
@ActiveProfiles("test")
class AlbumControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PhotoManagerFacade facade;

    @MockitoBean
    UserRepository userRepository;

    private UUID userId;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        now = Instant.now();
        User user = new User();
        user.setId(userId);
        user.setUsername("admin");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
    }

    // --- GET /api/albums ---

    @Test
    @WithMockUser("user")
    void listAlbums_returnsAlbumList_200() throws Exception {
        AlbumData album = new AlbumData(1L, "Wedding", null, now, 5L);
        when(facade.getAlbums(userId)).thenReturn(List.of(album));

        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].albumId").value(1))
                .andExpect(jsonPath("$[0].name").value("Wedding"))
                .andExpect(jsonPath("$[0].assetCount").value(5));
    }

    // --- POST /api/albums ---

    @Test
    @WithMockUser("user")
    void createAlbum_validRequest_returns201WithDto() throws Exception {
        AlbumData created = new AlbumData(2L, "Vacation 2025", null, now, 0L);
        when(facade.createAlbum(eq(userId), eq("Vacation 2025"), isNull())).thenReturn(created);

        CreateAlbumRequest req = new CreateAlbumRequest("Vacation 2025", null);
        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.albumId").value(2))
                .andExpect(jsonPath("$.name").value("Vacation 2025"))
                .andExpect(jsonPath("$.assetCount").value(0));
    }

    // --- GET /api/albums/{id} ---

    @Test
    @WithMockUser("user")
    void getAlbum_existingAlbum_returns200WithAlbumDto() throws Exception {
        AlbumData summary = new AlbumData(1L, "Wedding", "2024", now, 3L);
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 10L);
        PaginatedData<Asset> assets = new PaginatedData<>(List.of(asset), 0, 1, 1L);

        when(facade.getAlbumSummary(1L, userId)).thenReturn(summary);
        when(facade.getAlbumAssets(1L, userId, 0)).thenReturn(assets);

        mockMvc.perform(get("/api/albums/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumId").value(1))
                .andExpect(jsonPath("$.name").value("Wedding"))
                .andExpect(jsonPath("$.assets.items[0].fileName").value("photo.jpg"));
    }

    @Test
    @WithMockUser("user")
    void getAlbum_unknownAlbum_returns404() throws Exception {
        when(facade.getAlbumSummary(999L, userId)).thenThrow(new AlbumNotFoundException(999L));

        mockMvc.perform(get("/api/albums/999"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/albums/{id} ---

    @Test
    @WithMockUser("user")
    void updateAlbum_validRequest_returns200() throws Exception {
        AlbumData updated = new AlbumData(1L, "New Name", null, now, 3L);
        when(facade.updateAlbum(eq(1L), eq(userId), eq("New Name"), isNull())).thenReturn(updated);

        UpdateAlbumRequest req = new UpdateAlbumRequest("New Name", null);
        mockMvc.perform(put("/api/albums/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    // --- DELETE /api/albums/{id} ---

    @Test
    @WithMockUser("user")
    void deleteAlbum_existingAlbum_returns204() throws Exception {
        doNothing().when(facade).deleteAlbum(1L, userId);

        mockMvc.perform(delete("/api/albums/1"))
                .andExpect(status().isNoContent());
    }

    // --- POST /api/albums/{id}/assets ---

    @Test
    @WithMockUser("user")
    void addAssets_validRequest_returns204() throws Exception {
        doNothing().when(facade).addAssetsToAlbum(eq(1L), eq(userId), anyList());

        AlbumAssetIdsRequest req = new AlbumAssetIdsRequest(List.of(101L, 102L));
        mockMvc.perform(post("/api/albums/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    // --- DELETE /api/albums/{id}/assets ---

    @Test
    @WithMockUser("user")
    void removeAssets_validRequest_returns204() throws Exception {
        doNothing().when(facade).removeAssetsFromAlbum(eq(1L), eq(userId), anyList());

        AlbumAssetIdsRequest req = new AlbumAssetIdsRequest(List.of(102L));
        mockMvc.perform(delete("/api/albums/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }

    private Asset buildAsset(Folder folder, String fileName, Long id) {
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setFileSize(1000L);
        asset.setHash("abc123");
        asset.setThumbnailCreationDateTime(java.time.LocalDateTime.now());
        return asset;
    }
}
