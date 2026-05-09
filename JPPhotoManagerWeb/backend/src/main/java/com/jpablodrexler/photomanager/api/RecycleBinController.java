package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.dto.AssetDto;
import com.jpablodrexler.photomanager.api.dto.RecycleBinPurgeRequest;
import com.jpablodrexler.photomanager.api.dto.RecycleBinRestoreRequest;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {

    private final PhotoManagerFacade facade;

    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> listDeleted(
            @RequestParam(defaultValue = "0") int page) {
        PaginatedData<Asset> data = facade.getRecycleBin(page);
        PaginatedData<AssetDto> result = new PaginatedData<>(
                data.getItems().stream().map(this::toDto).collect(Collectors.toList()),
                data.getPageIndex(),
                data.getTotalPages(),
                data.getTotalItems());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restore(@Valid @RequestBody RecycleBinRestoreRequest body) {
        facade.restoreAssets(body.assetIds());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> purge(@RequestBody(required = false) RecycleBinPurgeRequest body) {
        List<Long> ids = (body != null) ? body.assetIds() : null;
        facade.purgeRecycleBin(ids);
        return ResponseEntity.noContent().build();
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
