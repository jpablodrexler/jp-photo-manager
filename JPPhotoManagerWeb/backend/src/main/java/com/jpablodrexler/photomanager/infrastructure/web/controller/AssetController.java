package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
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
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.catalog.GetDuplicatedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AddTagRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.BulkTagRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.DownloadAssetsRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.ExifMetadataDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.MoveAssetsRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RateAssetRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RenameAssetsRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RenameAssetsResponse;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RenamePreviewDto;
import com.jpablodrexler.photomanager.application.dto.RenameAssetsResult;
import com.jpablodrexler.photomanager.infrastructure.web.dto.TimelineGroupDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Tag(name = "Assets", description = "Photo and video asset management")
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final GetAssetsUseCase getAssetsUseCase;
    private final GetAssetsTimelineUseCase getAssetsTimelineUseCase;
    private final GetAssetImageUseCase getAssetImageUseCase;
    private final GetAssetExifUseCase getAssetExifUseCase;
    private final DownloadAssetsUseCase downloadAssetsUseCase;
    private final RateAssetUseCase rateAssetUseCase;
    private final MoveAssetsUseCase moveAssetsUseCase;
    private final RenameAssetsUseCase renameAssetsUseCase;
    private final UploadAssetUseCase uploadAssetUseCase;
    private final DeleteAssetsUseCase deleteAssetsUseCase;
    private final CropAssetUseCase cropAssetUseCase;
    private final CatalogAssetsUseCase catalogAssetsUseCase;
    private final GetDuplicatedAssetsUseCase getDuplicatedAssetsUseCase;
    private final AddTagToAssetUseCase addTagToAssetUseCase;
    private final RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    private final BulkAddTagUseCase bulkAddTagUseCase;
    private final BulkRemoveTagUseCase bulkRemoveTagUseCase;
    private final ThumbnailPort thumbnailPort;
    private final FolderRepository folderRepository;
    private final AssetWebMapper assetWebMapper;
    private final MeterRegistry meterRegistry;

    private final AtomicInteger sseConnectionCount = new AtomicInteger(0);

    @Value("${photomanager.max-download-assets:500}")
    private int maxDownloadAssets;

    @PostConstruct
    private void initMetrics() {
        Gauge.builder("photomanager_active_sse_connections", sseConnectionCount, AtomicInteger::get)
                .description("Active SSE connections")
                .register(meterRegistry);
    }

    @Operation(summary = "List assets in a folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated asset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> getAssets(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "FILE_NAME") SortCriteria sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String tags) {
        Long folderId = folderRepository.findByPath(folderPath).map(Folder::getFolderId).orElse(null);
        Set<String> tagSet = parseTags(tags);
        AssetFilter filter = new AssetFilter(folderId, search, dateFrom, dateTo, minRating, sort, page, 50, false, tagSet);
        PaginatedResult<Asset> result = getAssetsUseCase.execute(filter);
        List<AssetDto> dtos = result.items().stream().map(assetWebMapper::toDto).collect(Collectors.toList());
        int totalPages = result.pageSize() > 0 ? (int) Math.ceil((double) result.total() / result.pageSize()) : 0;
        PaginatedData<AssetDto> data = new PaginatedData<>(dtos, page, totalPages, result.total());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "List assets grouped by date (timeline view)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated timeline groups"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/timeline")
    public ResponseEntity<PaginatedData<TimelineGroupDto>> getTimeline(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String tags) {
        Long folderId = folderRepository.findByPath(folderPath).map(Folder::getFolderId).orElse(null);
        Set<String> tagSet = parseTags(tags);
        AssetFilter filter = new AssetFilter(folderId, search, dateFrom, dateTo, minRating, null, page, 0, false, tagSet);
        PaginatedResult<com.jpablodrexler.photomanager.domain.model.TimelineGroup> result = getAssetsTimelineUseCase.execute(filter);
        List<TimelineGroupDto> dtos = result.items().stream().map(assetWebMapper::toTimelineGroupDto).collect(Collectors.toList());
        int totalPages = result.pageSize() > 0 ? (int) Math.ceil((double) result.total() / result.pageSize()) : 0;
        PaginatedData<TimelineGroupDto> data = new PaginatedData<>(dtos, page, totalPages, result.total());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Get thumbnail image for an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JPEG thumbnail"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Thumbnail not found")
    })
    @GetMapping("/{assetId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long assetId) {
        byte[] data = thumbnailPort.loadThumbnail(assetId + ".bin");
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(data);
    }

    @Operation(summary = "Get full-size image for an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Full-size image bytes"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "415", description = "Unsupported media type")
    })
    @GetMapping("/{assetId}/image")
    public ResponseEntity<byte[]> getFullImage(@PathVariable Long assetId) {
        try {
            AssetImage image = getAssetImageUseCase.execute(assetId);
            MediaType mediaType = detectMediaType(image.bytes());
            if (mediaType == null) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }
            return ResponseEntity.ok().contentType(mediaType).body(image.bytes());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Catalog assets via SSE stream")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SSE stream of catalog progress events"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/catalog")
    public SseEmitter catalogAssets() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseConnectionCount.incrementAndGet();
        emitter.onCompletion(sseConnectionCount::decrementAndGet);
        emitter.onTimeout(sseConnectionCount::decrementAndGet);
        emitter.onError(t -> sseConnectionCount.decrementAndGet());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                catalogAssetsUseCase.execute(notification -> {
                    try {
                        emitter.send(SseEmitter.event().name("catalog").data(notification));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }).get();
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }

    @Operation(summary = "Move or copy assets to a destination folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operation result"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/move")
    public ResponseEntity<Boolean> moveAssets(@Valid @RequestBody MoveAssetsRequest request) {
        boolean result = moveAssetsUseCase.execute(request.getAssetIds(), request.getDestinationFolderPath(),
                request.isPreserveOriginal());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Preview or apply a pattern-based batch rename for selected assets")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview or apply result"),
        @ApiResponse(responseCode = "400", description = "Validation error or name collision"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/rename")
    public ResponseEntity<RenameAssetsResponse> renameAssets(@Valid @RequestBody RenameAssetsRequest request) {
        try {
            RenameAssetsResult result = renameAssetsUseCase.execute(
                    request.assetIds(), request.pattern(), request.applied());
            List<RenamePreviewDto> previews = result.previews().stream()
                    .map(p -> new RenamePreviewDto(p.assetId(), p.oldName(), p.newName()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new RenameAssetsResponse(previews, result.applied()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Download selected assets as a ZIP file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ZIP archive of selected assets"),
        @ApiResponse(responseCode = "400", description = "Too many assets requested"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/download")
    public void downloadAssets(@Valid @RequestBody DownloadAssetsRequest request, HttpServletResponse response)
            throws IOException {
        if (request.getAssetIds().size() > maxDownloadAssets) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"photos.zip\"");
        downloadAssetsUseCase.execute(request.getAssetIds(), response.getOutputStream());
    }

    @Operation(summary = "Rate an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Rating saved"),
        @ApiResponse(responseCode = "400", description = "Invalid rating value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    @PatchMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rateAsset(@PathVariable Long id, @Valid @RequestBody RateAssetRequest body) {
        rateAssetUseCase.execute(id, body.rating());
    }

    @Operation(summary = "Remove assets from catalog (optionally delete files)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assets removed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteAssets(@RequestParam Long[] assetIds,
                                              @RequestParam(defaultValue = "false") boolean deleteFiles) {
        deleteAssetsUseCase.execute(assetIds, deleteFiles);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get groups of duplicated assets")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of duplicate asset groups"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/duplicates")
    public ResponseEntity<List<List<AssetDto>>> getDuplicatedAssets() {
        List<List<Asset>> duplicates = getDuplicatedAssetsUseCase.execute();
        List<List<AssetDto>> result = duplicates.stream()
                .map(group -> group.stream().map(assetWebMapper::toDto).collect(Collectors.toList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get EXIF metadata for an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "EXIF metadata"),
        @ApiResponse(responseCode = "204", description = "No EXIF data available"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    @GetMapping("/{assetId}/exif")
    public ResponseEntity<ExifMetadataDto> getExifMetadata(@PathVariable Long assetId) {
        try {
            AssetExif exif = getAssetExifUseCase.execute(assetId);
            ExifMetadataDto dto = ExifMetadataDto.from(exif);
            if (dto == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(dto);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/webp");
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp");

    @Operation(summary = "Upload a new asset to a folder")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Asset created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Destination folder not found"),
        @ApiResponse(responseCode = "415", description = "Unsupported media type")
    })
    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(@RequestPart("file") MultipartFile file,
                                                 @RequestPart("folderPath") String folderPath) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        // Strip any directory prefix the client may have embedded in the filename (path traversal defence).
        // Paths.get().getFileName() returns only the last component; replacing '\' first handles Windows paths
        // sent by any client regardless of the server's OS.
        java.nio.file.Path filenamePath = originalFilename != null
                ? Paths.get(originalFilename.replace('\\', '/')).getFileName()
                : null;
        String safeFilename = (filenamePath != null) ? filenamePath.toString() : null;
        if (safeFilename == null || safeFilename.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String extension = safeFilename.contains(".")
                ? safeFilename.substring(safeFilename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)
                || !ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        try {
            Asset asset = uploadAssetUseCase.execute(folderPath, safeFilename, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(assetWebMapper.toDto(asset));
        } catch (FolderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Crop an asset to a social media format and save as a new asset")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cropped asset created"),
        @ApiResponse(responseCode = "400", description = "Invalid crop coordinates or format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "500", description = "Image processing error")
    })
    @PostMapping("/{id}/crop")
    public ResponseEntity<AssetDto> cropAsset(@PathVariable Long id, @RequestBody CropAssetRequest request) {
        try {
            Asset asset = cropAssetUseCase.execute(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(assetWebMapper.toDto(asset));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Add a tag to an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tag added"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    @PostMapping("/{id}/tags")
    public ResponseEntity<Void> addTag(@PathVariable Long id, @Valid @RequestBody AddTagRequest body) {
        addTagToAssetUseCase.execute(id, body.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove a tag from an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag removed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Asset or tag not found")
    })
    @DeleteMapping("/{id}/tags")
    public ResponseEntity<Void> removeTag(@PathVariable Long id, @RequestParam String name) {
        try {
            removeTagFromAssetUseCase.execute(id, name);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add a tag to multiple assets at once")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag added to all assets"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/tags/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkAddTag(@Valid @RequestBody BulkTagRequest body) {
        bulkAddTagUseCase.execute(body.assetIds(), body.name());
    }

    @Operation(summary = "Remove a tag from multiple assets at once")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag removed from all assets"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/tags/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkRemoveTag(@Valid @RequestBody BulkTagRequest body) {
        bulkRemoveTagUseCase.execute(body.assetIds(), body.name());
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return null;
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }

    private MediaType detectMediaType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (bytes.length >= 4
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8') {
            return MediaType.IMAGE_GIF;
        }
        return null;
    }
}
