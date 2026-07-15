package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.album.AddAssetsToAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.CreateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.DeleteAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumSummaryUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.RemoveAssetsFromAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.UpdateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.exception.SmartAlbumMembershipException;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AlbumAssetIdsRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreateAlbumRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.UpdateAlbumRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AlbumWebMapper;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
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
    GetAlbumsUseCase getAlbumsUseCase;
    @MockitoBean
    CreateAlbumUseCase createAlbumUseCase;
    @MockitoBean
    GetAlbumSummaryUseCase getAlbumSummaryUseCase;
    @MockitoBean
    GetAlbumAssetsUseCase getAlbumAssetsUseCase;
    @MockitoBean
    UpdateAlbumUseCase updateAlbumUseCase;
    @MockitoBean
    DeleteAlbumUseCase deleteAlbumUseCase;
    @MockitoBean
    AddAssetsToAlbumUseCase addAssetsToAlbumUseCase;
    @MockitoBean
    RemoveAssetsFromAlbumUseCase removeAssetsFromAlbumUseCase;
    @MockitoBean
    GetCurrentUserUseCase getCurrentUserUseCase;
    @MockitoBean
    AlbumWebMapper albumWebMapper;
    @MockitoBean
    AssetWebMapper assetWebMapper;

    private UUID userId;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        now = Instant.now();
        User user = new User();
        user.setId(userId);
        user.setUsername("admin");
        when(getCurrentUserUseCase.execute()).thenReturn(user);
    }

    // --- GET /api/albums ---

    @Test
    @WithMockUser("user")
    void listAlbums_returnsAlbumList_200() throws Exception {
        AlbumData album = new AlbumData(1L, "Wedding", null, now, 5L, null);
        AlbumSummaryResponseDto dto = new AlbumSummaryResponseDto();
        dto.setAlbumId(1L);
        dto.setName("Wedding");
        dto.setAssetCount(5L);

        when(getAlbumsUseCase.execute(userId)).thenReturn(List.of(album));
        when(albumWebMapper.toSummaryDto(album)).thenReturn(dto);

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
        AlbumData created = new AlbumData(2L, "Vacation 2025", null, now, 0L, null);
        AlbumSummaryResponseDto dto = new AlbumSummaryResponseDto();
        dto.setAlbumId(2L);
        dto.setName("Vacation 2025");
        dto.setAssetCount(0L);

        when(createAlbumUseCase.execute(eq(userId), eq("Vacation 2025"), isNull(), isNull())).thenReturn(created);
        when(albumWebMapper.toSummaryDto(created)).thenReturn(dto);

        CreateAlbumRequestDto req = new CreateAlbumRequestDto("Vacation 2025", null, null);
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
        AlbumData summary = new AlbumData(1L, "Wedding", "2024", now, 3L, null);
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 10L);
        PaginatedResult<Asset> assets = new PaginatedResult<>(List.of(asset), 1L, 0, 50);

        AssetResponseDto assetDto = new AssetResponseDto();
        assetDto.setAssetId(10L);
        assetDto.setFileName("photo.jpg");

        AlbumResponseDto albumDto = new AlbumResponseDto();
        albumDto.setAlbumId(1L);
        albumDto.setName("Wedding");
        albumDto.setDescription("2024");
        albumDto.setCreatedAt(now);
        albumDto.setAssets(new PaginatedData<>(List.of(assetDto), 0, 1, 1L));

        when(getAlbumSummaryUseCase.execute(1L, userId)).thenReturn(summary);
        when(getAlbumAssetsUseCase.execute(1L, userId, 0)).thenReturn(assets);
        when(assetWebMapper.toDto(asset)).thenReturn(assetDto);
        when(albumWebMapper.toDto(eq(summary), any())).thenReturn(albumDto);

        mockMvc.perform(get("/api/albums/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumId").value(1))
                .andExpect(jsonPath("$.name").value("Wedding"))
                .andExpect(jsonPath("$.assets.items[0].fileName").value("photo.jpg"));
    }

    @Test
    @WithMockUser("user")
    void getAlbum_unknownAlbum_returns404() throws Exception {
        when(getAlbumSummaryUseCase.execute(999L, userId)).thenThrow(new AlbumNotFoundException(999L));

        mockMvc.perform(get("/api/albums/999"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/albums/{id} ---

    @Test
    @WithMockUser("user")
    void updateAlbum_validRequest_returns200() throws Exception {
        AlbumData updated = new AlbumData(1L, "New Name", null, now, 3L, null);
        AlbumSummaryResponseDto dto = new AlbumSummaryResponseDto();
        dto.setAlbumId(1L);
        dto.setName("New Name");

        when(updateAlbumUseCase.execute(eq(1L), eq(userId), eq("New Name"), isNull(), isNull())).thenReturn(updated);
        when(albumWebMapper.toSummaryDto(updated)).thenReturn(dto);

        UpdateAlbumRequestDto req = new UpdateAlbumRequestDto("New Name", null, null);
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
        doNothing().when(deleteAlbumUseCase).execute(1L, userId);

        mockMvc.perform(delete("/api/albums/1"))
                .andExpect(status().isNoContent());
    }

    // --- POST /api/albums/{id}/assets ---

    @Test
    @WithMockUser("user")
    void addAssets_validRequest_returns204() throws Exception {
        doNothing().when(addAssetsToAlbumUseCase).execute(eq(1L), eq(userId), anyList());

        AlbumAssetIdsRequestDto req = new AlbumAssetIdsRequestDto(List.of(101L, 102L));
        mockMvc.perform(post("/api/albums/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    // --- DELETE /api/albums/{id}/assets ---

    @Test
    @WithMockUser("user")
    void removeAssets_validRequest_returns204() throws Exception {
        doNothing().when(removeAssetsFromAlbumUseCase).execute(eq(1L), eq(userId), anyList());

        AlbumAssetIdsRequestDto req = new AlbumAssetIdsRequestDto(List.of(102L));
        mockMvc.perform(delete("/api/albums/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    // --- POST /api/albums with filterJson ---

    @Test
    @WithMockUser("user")
    void createAlbum_withFilterJson_returns201WithFilterJson() throws Exception {
        AlbumFilterJson filterJson = new AlbumFilterJson(null, null, null, 4);
        AlbumData created = new AlbumData(3L, "Top Picks", null, now, 0L, "{\"minRating\":4}");
        AlbumSummaryResponseDto dto = new AlbumSummaryResponseDto();
        dto.setAlbumId(3L);
        dto.setName("Top Picks");
        dto.setFilterJson(filterJson);

        when(createAlbumUseCase.execute(eq(userId), eq("Top Picks"), isNull(), any())).thenReturn(created);
        when(albumWebMapper.toSummaryDto(created)).thenReturn(dto);

        CreateAlbumRequestDto req = new CreateAlbumRequestDto("Top Picks", null, filterJson);
        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.albumId").value(3))
                .andExpect(jsonPath("$.filterJson.minRating").value(4));
    }

    @Test
    @WithMockUser("user")
    void createAlbum_withEmptyFilterJson_returns400() throws Exception {
        CreateAlbumRequestDto req = new CreateAlbumRequestDto("Bad Smart", null, new AlbumFilterJson(null, null, null, null));
        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("user")
    void updateAlbum_withNullFilterJson_returns200WithNullFilterJson() throws Exception {
        AlbumData updated = new AlbumData(1L, "Vacation Static", null, now, 2L, null);
        AlbumSummaryResponseDto dto = new AlbumSummaryResponseDto();
        dto.setAlbumId(1L);
        dto.setName("Vacation Static");
        dto.setFilterJson(null);

        when(updateAlbumUseCase.execute(eq(1L), eq(userId), eq("Vacation Static"), isNull(), isNull())).thenReturn(updated);
        when(albumWebMapper.toSummaryDto(updated)).thenReturn(dto);

        UpdateAlbumRequestDto req = new UpdateAlbumRequestDto("Vacation Static", null, null);
        mockMvc.perform(put("/api/albums/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterJson").doesNotExist());
    }

    @Test
    @WithMockUser("user")
    void addAssets_smartAlbum_returns422WithCode() throws Exception {
        doThrow(new SmartAlbumMembershipException("add"))
                .when(addAssetsToAlbumUseCase).execute(eq(7L), eq(userId), anyList());

        AlbumAssetIdsRequestDto req = new AlbumAssetIdsRequestDto(List.of(101L, 102L));
        mockMvc.perform(post("/api/albums/7/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SMART_ALBUM_MEMBERSHIP_FORBIDDEN"));
    }

    @Test
    @WithMockUser("user")
    void removeAssets_smartAlbum_returns422() throws Exception {
        doThrow(new SmartAlbumMembershipException("remove"))
                .when(removeAssetsFromAlbumUseCase).execute(eq(7L), eq(userId), anyList());

        AlbumAssetIdsRequestDto req = new AlbumAssetIdsRequestDto(List.of(101L));
        mockMvc.perform(delete("/api/albums/7/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SMART_ALBUM_MEMBERSHIP_FORBIDDEN"));
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
