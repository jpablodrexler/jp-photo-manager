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
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.DownloadAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.MoveAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
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
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final GetAssetsUseCase getAssetsUseCase;
    private final GetAssetImageUseCase getAssetImageUseCase;
    private final GetAssetExifUseCase getAssetExifUseCase;
    private final DownloadAssetsUseCase downloadAssetsUseCase;
    private final RateAssetUseCase rateAssetUseCase;
    private final MoveAssetsUseCase moveAssetsUseCase;
    private final UploadAssetUseCase uploadAssetUseCase;
    private final DeleteAssetsUseCase deleteAssetsUseCase;
    private final CatalogAssetsUseCase catalogAssetsUseCase;
    private final GetDuplicatedAssetsUseCase getDuplicatedAssetsUseCase;
    private final AddTagToAssetUseCase addTagToAssetUseCase;
    private final RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    private final BulkAddTagUseCase bulkAddTagUseCase;
    private final BulkRemoveTagUseCase bulkRemoveTagUseCase;
    private final ThumbnailPort thumbnailPort;
    private final FolderRepository folderRepository;
    private final AssetWebMapper assetWebMapper;

    @Value("${photomanager.max-download-assets:500}")
    private int maxDownloadAssets;

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

    @GetMapping("/{assetId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long assetId) {
        byte[] data = thumbnailPort.loadThumbnail(assetId + ".bin");
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(data);
    }

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

    @GetMapping("/catalog")
    public SseEmitter catalogAssets() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
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

    @PostMapping("/move")
    public ResponseEntity<Boolean> moveAssets(@Valid @RequestBody MoveAssetsRequest request) {
        boolean result = moveAssetsUseCase.execute(request.getAssetIds(), request.getDestinationFolderPath(),
                request.isPreserveOriginal());
        return ResponseEntity.ok(result);
    }

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

    @PatchMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rateAsset(@PathVariable Long id, @Valid @RequestBody RateAssetRequest body) {
        rateAssetUseCase.execute(id, body.rating());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAssets(@RequestParam Long[] assetIds,
                                              @RequestParam(defaultValue = "false") boolean deleteFiles) {
        deleteAssetsUseCase.execute(assetIds, deleteFiles);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<List<AssetDto>>> getDuplicatedAssets() {
        List<List<Asset>> duplicates = getDuplicatedAssetsUseCase.execute();
        List<List<AssetDto>> result = duplicates.stream()
                .map(group -> group.stream().map(assetWebMapper::toDto).collect(Collectors.toList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

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

    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(@RequestPart("file") MultipartFile file,
                                                 @RequestPart("folderPath") String folderPath) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)
                || !ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        try {
            Asset asset = uploadAssetUseCase.execute(folderPath, originalFilename, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(assetWebMapper.toDto(asset));
        } catch (FolderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<Void> addTag(@PathVariable Long id, @Valid @RequestBody AddTagRequest body) {
        addTagToAssetUseCase.execute(id, body.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/tags")
    public ResponseEntity<Void> removeTag(@PathVariable Long id, @RequestParam String name) {
        try {
            removeTagFromAssetUseCase.execute(id, name);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/tags/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkAddTag(@Valid @RequestBody BulkTagRequest body) {
        bulkAddTagUseCase.execute(body.assetIds(), body.name());
    }

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
