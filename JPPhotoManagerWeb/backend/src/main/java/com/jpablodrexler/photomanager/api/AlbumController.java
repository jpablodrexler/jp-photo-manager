package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.dto.AlbumAssetIdsRequest;
import com.jpablodrexler.photomanager.api.dto.AlbumDto;
import com.jpablodrexler.photomanager.api.dto.AlbumSummaryDto;
import com.jpablodrexler.photomanager.api.dto.AssetDto;
import com.jpablodrexler.photomanager.api.dto.CreateAlbumRequest;
import com.jpablodrexler.photomanager.api.dto.UpdateAlbumRequest;
import com.jpablodrexler.photomanager.api.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final PhotoManagerFacade facade;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AlbumSummaryDto>> listAlbums() {
        List<AlbumData> albums = facade.getAlbums(resolveUserId());
        List<AlbumSummaryDto> result = albums.stream().map(this::toSummaryDto).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AlbumSummaryDto> createAlbum(@Valid @RequestBody CreateAlbumRequest request) {
        AlbumData album = facade.createAlbum(resolveUserId(), request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummaryDto(album));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlbumDto> getAlbum(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page) {
        try {
            UUID userId = resolveUserId();
            AlbumData summary = facade.getAlbumSummary(id, userId);
            PaginatedData<Asset> assetsPage = facade.getAlbumAssets(id, userId, page);
            PaginatedData<AssetDto> assetDtos = new PaginatedData<>(
                    assetsPage.getItems().stream().map(this::toAssetDto).collect(Collectors.toList()),
                    assetsPage.getPageIndex(),
                    assetsPage.getTotalPages(),
                    assetsPage.getTotalItems());
            AlbumDto dto = new AlbumDto();
            dto.setAlbumId(summary.albumId());
            dto.setName(summary.name());
            dto.setDescription(summary.description());
            dto.setCreatedAt(summary.createdAt());
            dto.setAssets(assetDtos);
            return ResponseEntity.ok(dto);
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlbumSummaryDto> updateAlbum(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlbumRequest request) {
        try {
            AlbumData updated = facade.updateAlbum(id, resolveUserId(), request.name(), request.description());
            return ResponseEntity.ok(toSummaryDto(updated));
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        try {
            facade.deleteAlbum(id, resolveUserId());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/assets")
    public ResponseEntity<Void> addAssets(
            @PathVariable Long id,
            @Valid @RequestBody AlbumAssetIdsRequest request) {
        try {
            facade.addAssetsToAlbum(id, resolveUserId(), request.assetIds());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/assets")
    public ResponseEntity<Void> removeAssets(
            @PathVariable Long id,
            @Valid @RequestBody AlbumAssetIdsRequest request) {
        try {
            facade.removeAssetsFromAlbum(id, resolveUserId(), request.assetIds());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UUID resolveUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));
    }

    private AlbumSummaryDto toSummaryDto(AlbumData data) {
        AlbumSummaryDto dto = new AlbumSummaryDto();
        dto.setAlbumId(data.albumId());
        dto.setName(data.name());
        dto.setDescription(data.description());
        dto.setCreatedAt(data.createdAt());
        dto.setAssetCount(data.assetCount());
        return dto;
    }

    private AssetDto toAssetDto(Asset asset) {
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
