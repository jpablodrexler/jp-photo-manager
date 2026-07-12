package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.AssetImage;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.TimelineGroupResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.ExifMetadataResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.RenamePreviewResponseDto;
import com.jpablodrexler.photomanager.domain.model.CropAssetRequest;
import com.jpablodrexler.photomanager.domain.port.in.asset.CropAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DownloadAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsTimelineUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.MoveAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RenameAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.ReprocessAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.GetDuplicatedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.domain.model.RenameAssetsResult;
import com.jpablodrexler.photomanager.domain.model.RenamePreview;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.MoveAssetsRequestDto;

import java.time.LocalDate;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(AssetController.class)
@ActiveProfiles("test")
class AssetControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AssetController assetController;

    @MockitoBean
    CropAssetUseCase cropAssetUseCase;
    @MockitoBean
    GetAssetsUseCase getAssetsUseCase;
    @MockitoBean
    GetAssetsTimelineUseCase getAssetsTimelineUseCase;
    @MockitoBean
    GetAssetImageUseCase getAssetImageUseCase;
    @MockitoBean
    GetAssetExifUseCase getAssetExifUseCase;
    @MockitoBean
    DownloadAssetsUseCase downloadAssetsUseCase;
    @MockitoBean
    RateAssetUseCase rateAssetUseCase;
    @MockitoBean
    MoveAssetsUseCase moveAssetsUseCase;
    @MockitoBean
    RenameAssetsUseCase renameAssetsUseCase;
    @MockitoBean
    UploadAssetUseCase uploadAssetUseCase;
    @MockitoBean
    ReprocessAssetUseCase reprocessAssetUseCase;
    @MockitoBean
    DeleteAssetsUseCase deleteAssetsUseCase;
    @MockitoBean
    CatalogAssetsUseCase catalogAssetsUseCase;
    @MockitoBean
    GetDuplicatedAssetsUseCase getDuplicatedAssetsUseCase;
    @MockitoBean
    AddTagToAssetUseCase addTagToAssetUseCase;
    @MockitoBean
    RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    @MockitoBean
    BulkAddTagUseCase bulkAddTagUseCase;
    @MockitoBean
    BulkRemoveTagUseCase bulkRemoveTagUseCase;
    @MockitoBean
    ThumbnailPort thumbnailPort;
    @MockitoBean
    FolderRepository folderRepository;
    @MockitoBean
    AssetWebMapper assetWebMapper;
    @MockitoBean
    MeterRegistry meterRegistry;
    @MockitoBean
    KafkaProgressRegistry kafkaProgressRegistry;
    @MockitoBean
    UserRepository userRepository;

    @BeforeEach
    void resetSseCounter() {
        AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(assetController, "sseConnectionCount");
        if (count != null) {
            count.set(0);
        }
    }

    // --- GET /api/assets ---

    @Test
    void getAssets_validRequest_returns200WithPaginatedItems() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 1L);
        PaginatedResult<Asset> page = new PaginatedResult<>(List.of(asset), 1L, 0, 50);

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(page);
        when(assetWebMapper.toDto(asset)).thenReturn(buildAssetDto("photo.jpg", 1L));

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.pageIndex").value(0));
    }

    @Test
    void getAssets_emptyFolder_returns200WithEmptyItems() throws Exception {
        PaginatedResult<Asset> page = new PaginatedResult<>(List.of(), 0L, 0, 50);
        when(folderRepository.findByPath("/empty")).thenReturn(Optional.empty());
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/empty")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // --- GET /api/assets/{id}/thumbnail ---

    @Test
    void getThumbnail_thumbnailExists_returns200WithJpegBytes() throws Exception {
        when(thumbnailPort.loadThumbnail("42.bin"))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8});

        mockMvc.perform(get("/api/assets/42/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getThumbnail_thumbnailExists_returnsCacheControlImmutable() throws Exception {
        when(thumbnailPort.loadThumbnail("42.bin"))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8});

        mockMvc.perform(get("/api/assets/42/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(header().string(org.springframework.http.HttpHeaders.CACHE_CONTROL,
                        "public, max-age=31536000, immutable"));
    }

    @Test
    void getThumbnail_thumbnailMissing_returns404() throws Exception {
        when(thumbnailPort.loadThumbnail("99.bin")).thenReturn(null);

        mockMvc.perform(get("/api/assets/99/thumbnail"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/assets/{id}/image ---

    @Test
    void getFullImage_jpegMagicBytes_returns200WithJpegContentType() throws Exception {
        when(getAssetImageUseCase.execute(1L, null))
                .thenReturn(new AssetImage(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "photo.jpg"));

        mockMvc.perform(get("/api/assets/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getFullImage_assetNotFound_returns404() throws Exception {
        when(getAssetImageUseCase.execute(99L, null)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/assets/99/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFullImage_pngMagicBytes_returns200WithPngContentType() throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'};
        when(getAssetImageUseCase.execute(2L, null))
                .thenReturn(new AssetImage(pngBytes, "image.png"));

        mockMvc.perform(get("/api/assets/2/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getFullImage_gifMagicBytes_returns200WithGifContentType() throws Exception {
        byte[] gifBytes = {'G', 'I', 'F', '8', '9', 'a'};
        when(getAssetImageUseCase.execute(3L, null))
                .thenReturn(new AssetImage(gifBytes, "anim.gif"));

        mockMvc.perform(get("/api/assets/3/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_GIF));
    }

    @Test
    void getFullImage_unknownMagicBytes_returns415() throws Exception {
        when(getAssetImageUseCase.execute(4L, null))
                .thenReturn(new AssetImage(new byte[]{1, 2, 3}, "disguised.jpg"));

        mockMvc.perform(get("/api/assets/4/image"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getFullImage_pngMagicBytesWithJpgExtension_returns200WithPngContentType() throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'};
        when(getAssetImageUseCase.execute(5L, null))
                .thenReturn(new AssetImage(pngBytes, "misleading.jpg"));

        mockMvc.perform(get("/api/assets/5/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    // --- GET /api/assets/catalog (SSE) ---

    @Test
    void catalogAssets_initiatesAsyncProcessing_returns200() throws Exception {
        when(catalogAssetsUseCase.execute(anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        doAnswer((InvocationOnMock inv) -> { inv.<SseEmitter>getArgument(1).complete(); return null; })
                .when(kafkaProgressRegistry).registerEmitter(anyLong(), any(SseEmitter.class));

        MvcResult result = mockMvc.perform(get("/api/assets/catalog"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    void catalogAssets_sseGauge_incrementsOnOpen() throws Exception {
        when(catalogAssetsUseCase.execute(anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(assetController, "sseConnectionCount");
        assertThat(count).isNotNull();
        assertThat(count.get()).isEqualTo(0);

        mockMvc.perform(get("/api/assets/catalog"))
                .andExpect(request().asyncStarted())
                .andReturn();
        assertThat(count.get()).isGreaterThanOrEqualTo(1);
    }

    // --- POST /api/assets/move ---

    @Test
    void moveAssets_validRequest_returns200WithTrue() throws Exception {
        when(moveAssetsUseCase.execute(any(), eq("/dest"), eq(false))).thenReturn(true);

        MoveAssetsRequestDto request = new MoveAssetsRequestDto();
        request.setAssetIds(new Long[]{1L, 2L});
        request.setDestinationFolderPath("/dest");
        request.setPreserveOriginal(false);

        mockMvc.perform(post("/api/assets/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void moveAssets_moveFails_returns200WithFalse() throws Exception {
        when(moveAssetsUseCase.execute(any(), eq("/dest"), eq(false))).thenReturn(false);

        MoveAssetsRequestDto request = new MoveAssetsRequestDto();
        request.setAssetIds(new Long[]{1L});
        request.setDestinationFolderPath("/dest");
        request.setPreserveOriginal(false);

        mockMvc.perform(post("/api/assets/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // --- DELETE /api/assets ---

    @Test
    void deleteAssets_validRequest_returns204() throws Exception {
        doNothing().when(deleteAssetsUseCase).execute(any(), anyBoolean());

        mockMvc.perform(delete("/api/assets")
                        .param("assetIds", "1", "2")
                        .param("deleteFiles", "true"))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/assets/duplicates ---

    @Test
    void getDuplicatedAssets_duplicatesExist_returns200WithGroups() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset a1 = buildAsset(folder, "a.jpg", 1L);
        Asset a2 = buildAsset(folder, "b.jpg", 2L);
        when(getDuplicatedAssetsUseCase.execute()).thenReturn(List.of(List.of(a1, a2)));
        when(assetWebMapper.toDto(a1)).thenReturn(buildAssetDto("a.jpg", 1L));
        when(assetWebMapper.toDto(a2)).thenReturn(buildAssetDto("b.jpg", 2L));

        mockMvc.perform(get("/api/assets/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isArray())
                .andExpect(jsonPath("$[0].length()").value(2));
    }

    @Test
    void getDuplicatedAssets_noDuplicates_returns200WithEmptyList() throws Exception {
        when(getDuplicatedAssetsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/assets/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/assets/{assetId}/exif ---

    @Test
    void getExifMetadata_exifExists_returns200WithFields() throws Exception {
        AssetExif exif = new AssetExif();
        exif.setAssetId(42L);
        exif.setCameraMake("Canon");
        exif.setCameraModel("EOS 90D");
        exif.setIsoSpeed(400);

        when(getAssetExifUseCase.execute(42L)).thenReturn(exif);
        when(assetWebMapper.toDto(exif)).thenReturn(new ExifMetadataResponseDto(
                "Canon", "EOS 90D", null, null, null, 400, null, null, null, null, null, null, null));

        mockMvc.perform(get("/api/assets/42/exif"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cameraMake").value("Canon"))
                .andExpect(jsonPath("$.cameraModel").value("EOS 90D"))
                .andExpect(jsonPath("$.isoSpeed").value(400));
    }

    @Test
    void getExifMetadata_noExifRow_returns204() throws Exception {
        when(getAssetExifUseCase.execute(42L)).thenReturn(null);

        mockMvc.perform(get("/api/assets/42/exif"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getExifMetadata_assetNotFound_returns404() throws Exception {
        when(getAssetExifUseCase.execute(99L)).thenThrow(new java.util.NoSuchElementException("not found"));

        mockMvc.perform(get("/api/assets/99/exif"))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/assets/{id}/rating ---

    @Test
    void rateAsset_validRating_returns204AndCallsUseCase() throws Exception {
        doNothing().when(rateAssetUseCase).execute(42L, 4, null);

        mockMvc.perform(patch("/api/assets/42/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isNoContent());

        verify(rateAssetUseCase).execute(42L, 4, null);
    }

    @Test
    void rateAsset_ratingTooHigh_returns400() throws Exception {
        mockMvc.perform(patch("/api/assets/42/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateAsset_ratingNegative_returns400() throws Exception {
        mockMvc.perform(patch("/api/assets/42/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssets_withMinRating_callsUseCaseWithMinRating() throws Exception {
        PaginatedResult<Asset> page = new PaginatedResult<>(List.of(), 0L, 0, 50);
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(getAssetsUseCase.execute(any(AssetFilter.class))).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                        .param("folderPath", "/photos")
                        .param("page", "0")
                        .param("minRating", "3"))
                .andExpect(status().isOk());

        verify(getAssetsUseCase).execute(argThat(filter -> filter.minRating() != null && filter.minRating() == 3));
    }

    // --- GET /api/assets/timeline ---

    @Test
    void getTimeline_validFolder_returns200WithGroups() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg", 1L);
        TimelineGroup group = new TimelineGroup(LocalDate.of(2024, 5, 10), "May 10, 2024", List.of(asset));
        com.jpablodrexler.photomanager.domain.model.PaginatedResult<TimelineGroup> result =
                new com.jpablodrexler.photomanager.domain.model.PaginatedResult<>(List.of(group), 1L, 0, 30);

        com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto assetDto = buildAssetDto("photo.jpg", 1L);
        TimelineGroupResponseDto groupDto = new TimelineGroupResponseDto(LocalDate.of(2024, 5, 10), "May 10, 2024", List.of(assetDto));

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(getAssetsTimelineUseCase.execute(any(AssetFilter.class))).thenReturn(result);
        when(assetWebMapper.toTimelineGroupDto(group)).thenReturn(groupDto);

        mockMvc.perform(get("/api/assets/timeline")
                        .param("folderPath", "/photos")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].label").value("May 10, 2024"))
                .andExpect(jsonPath("$.items[0].assets[0].fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getTimeline_emptyFolder_returns200WithEmptyItems() throws Exception {
        com.jpablodrexler.photomanager.domain.model.PaginatedResult<TimelineGroup> result =
                new com.jpablodrexler.photomanager.domain.model.PaginatedResult<>(List.of(), 0L, 0, 30);

        when(folderRepository.findByPath("/empty")).thenReturn(Optional.empty());
        when(getAssetsTimelineUseCase.execute(any(AssetFilter.class))).thenReturn(result);

        mockMvc.perform(get("/api/assets/timeline")
                        .param("folderPath", "/empty")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    // --- POST /api/assets/{id}/crop ---

    @Test
    void cropAsset_validRequest_returns201WithCreatedAsset() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        Asset saved = buildAsset(folder, "photo_INSTAGRAM_POST.jpg", 2L);
        com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto savedDto = buildAssetDto("photo_INSTAGRAM_POST.jpg", 2L);

        when(cropAssetUseCase.execute(eq(1L), any(CropAssetRequest.class))).thenReturn(saved);
        when(assetWebMapper.toDto(saved)).thenReturn(savedDto);

        mockMvc.perform(post("/api/assets/1/crop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatKey\":\"INSTAGRAM_POST\",\"x\":0,\"y\":0,\"width\":100,\"height\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("photo_INSTAGRAM_POST.jpg"));
    }

    @Test
    void cropAsset_assetNotFound_returns404() throws Exception {
        when(cropAssetUseCase.execute(eq(99L), any(CropAssetRequest.class)))
                .thenThrow(new NoSuchElementException("Asset not found: 99"));

        mockMvc.perform(post("/api/assets/99/crop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatKey\":\"INSTAGRAM_POST\",\"x\":0,\"y\":0,\"width\":100,\"height\":100}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cropAsset_outOfBoundsCoordinates_returns400() throws Exception {
        when(cropAssetUseCase.execute(eq(1L), any(CropAssetRequest.class)))
                .thenThrow(new IllegalArgumentException("Crop coordinates out of bounds"));

        mockMvc.perform(post("/api/assets/1/crop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatKey\":\"INSTAGRAM_POST\",\"x\":9999,\"y\":9999,\"width\":100,\"height\":100}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/assets/rename ---

    @Test
    void renameAssets_previewOnly_returns200WithPreviews() throws Exception {
        RenamePreview preview = new RenamePreview(1L, "old.jpg", "new.jpg");
        RenameAssetsResult result = new RenameAssetsResult(List.of(preview), false);
        when(renameAssetsUseCase.execute(any(), eq("{original}.{ext}"), eq(false))).thenReturn(result);
        when(assetWebMapper.toDto(preview)).thenReturn(new RenamePreviewResponseDto(1L, "old.jpg", "new.jpg"));

        mockMvc.perform(post("/api/assets/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[1],\"pattern\":\"{original}.{ext}\",\"applied\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.previews[0].oldName").value("old.jpg"))
                .andExpect(jsonPath("$.previews[0].newName").value("new.jpg"));
    }

    @Test
    void renameAssets_applied_returns200WithAppliedTrue() throws Exception {
        RenamePreview preview = new RenamePreview(1L, "old.jpg", "new.jpg");
        RenameAssetsResult result = new RenameAssetsResult(List.of(preview), true);
        when(renameAssetsUseCase.execute(any(), eq("new.{ext}"), eq(true))).thenReturn(result);
        when(assetWebMapper.toDto(preview)).thenReturn(new RenamePreviewResponseDto(1L, "old.jpg", "new.jpg"));

        mockMvc.perform(post("/api/assets/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[1],\"pattern\":\"new.{ext}\",\"applied\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));
    }

    @Test
    void renameAssets_collision_returns400() throws Exception {
        when(renameAssetsUseCase.execute(any(), any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("ASSET_NAME_COLLISION"));

        mockMvc.perform(post("/api/assets/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[1,2],\"pattern\":\"{date:yyyy-MM-dd}.{ext}\",\"applied\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameAssets_emptyAssetList_returns400() throws Exception {
        mockMvc.perform(post("/api/assets/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[],\"pattern\":\"test.{ext}\",\"applied\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameAssets_invalidDateFormat_returns400() throws Exception {
        when(renameAssetsUseCase.execute(any(), any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("INVALID_DATE_FORMAT"));

        mockMvc.perform(post("/api/assets/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[1],\"pattern\":\"{date:INVALID!!!}\",\"applied\":false}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

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
        return asset;
    }

    private com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto buildAssetDto(String fileName, Long id) {
        com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto dto =
                new com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto();
        dto.setAssetId(id);
        dto.setFileName(fileName);
        return dto;
    }
}
