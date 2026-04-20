package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.dto.AssetDto;
import com.jpablodrexler.photomanager.api.dto.MoveAssetsRequest;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.infrastructure.service.ThumbnailStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssetController {

    private final PhotoManagerFacade facade;
    private final ThumbnailStorageService thumbnailStorageService;

    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> getAssets(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "FILE_NAME") SortCriteria sort) {
        PaginatedData<Asset> data = facade.getAssets(folderPath, page, sort);
        PaginatedData<AssetDto> result = new PaginatedData<>(
                data.getItems().stream().map(this::toDto).collect(Collectors.toList()),
                data.getPageIndex(),
                data.getTotalPages(),
                data.getTotalItems()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{assetId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long assetId) {
        byte[] data = thumbnailStorageService.loadThumbnail(assetId + ".bin");
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
    }

    @GetMapping("/{assetId}/image")
    public ResponseEntity<byte[]> getFullImage(@PathVariable Long assetId) {
        try {
            PhotoManagerFacade.AssetImage image = facade.getAssetImage(assetId);
            return ResponseEntity.ok()
                    .contentType(detectMediaType(image.fileName()))
                    .body(image.bytes());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType detectMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return MediaType.IMAGE_JPEG;
    }

    @GetMapping("/catalog")
    public SseEmitter catalogAssets() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                facade.catalogAssetsAsync(notification -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("catalog")
                                .data(notification));
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
        boolean result = facade.moveAssets(request.getAssetIds(), request.getDestinationFolderPath(),
                request.isPreserveOriginal());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAssets(
            @RequestParam Long[] assetIds,
            @RequestParam(defaultValue = "false") boolean deleteFiles) {
        facade.deleteAssets(assetIds, deleteFiles);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<List<AssetDto>>> getDuplicatedAssets() {
        List<List<Asset>> duplicates = facade.getDuplicatedAssets();
        List<List<AssetDto>> result = duplicates.stream()
                .map(group -> group.stream().map(this::toDto).collect(Collectors.toList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private AssetDto toDto(Asset asset) {
        AssetDto dto = new AssetDto();
        dto.setAssetId(asset.getAssetId());
        dto.setFolderId(asset.getFolder().getFolderId());
        dto.setFolderPath(asset.getFolder().getPath());
        dto.setFileName(asset.getFileName());
        dto.setFileSize(asset.getFileSize());
        dto.setPixelWidth(asset.getPixelWidth());
        dto.setPixelHeight(asset.getPixelHeight());
        dto.setThumbnailPixelWidth(asset.getThumbnailPixelWidth());
        dto.setThumbnailPixelHeight(asset.getThumbnailPixelHeight());
        dto.setImageRotation(asset.getImageRotation());
        dto.setThumbnailCreationDateTime(asset.getThumbnailCreationDateTime());
        dto.setHash(asset.getHash());
        dto.setFileCreationDateTime(asset.getFileCreationDateTime());
        dto.setFileModificationDateTime(asset.getFileModificationDateTime());
        dto.setThumbnailUrl("/api/assets/" + asset.getAssetId() + "/thumbnail");
        dto.setImageUrl("/api/assets/" + asset.getAssetId() + "/image");
        return dto;
    }
}
