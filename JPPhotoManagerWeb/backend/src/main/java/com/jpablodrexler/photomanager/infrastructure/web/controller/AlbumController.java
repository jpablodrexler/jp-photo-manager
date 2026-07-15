package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.album.AddAssetsToAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.CreateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.DeleteAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumSummaryUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.RemoveAssetsFromAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.UpdateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AlbumAssetIdsRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AlbumSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreateAlbumRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.UpdateAlbumRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AlbumWebMapper;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Albums", description = "Album creation and management")
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final GetAlbumsUseCase getAlbumsUseCase;
    private final CreateAlbumUseCase createAlbumUseCase;
    private final GetAlbumSummaryUseCase getAlbumSummaryUseCase;
    private final GetAlbumAssetsUseCase getAlbumAssetsUseCase;
    private final UpdateAlbumUseCase updateAlbumUseCase;
    private final DeleteAlbumUseCase deleteAlbumUseCase;
    private final AddAssetsToAlbumUseCase addAssetsToAlbumUseCase;
    private final RemoveAssetsFromAlbumUseCase removeAssetsFromAlbumUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AlbumWebMapper albumWebMapper;
    private final AssetWebMapper assetWebMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "List albums for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Album list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<AlbumSummaryResponseDto>> listAlbums() {
        List<AlbumData> albums = getAlbumsUseCase.execute(resolveUserId());
        return ResponseEntity.ok(albums.stream().map(albumWebMapper::toSummaryDto).collect(Collectors.toList()));
    }

    @Operation(summary = "Create a new album")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Album created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<AlbumSummaryResponseDto> createAlbum(@Valid @RequestBody CreateAlbumRequestDto request) {
        AlbumData album = createAlbumUseCase.execute(resolveUserId(), request.name(), request.description(), serializeFilterJson(request.filterJson()));
        return ResponseEntity.status(HttpStatus.CREATED).body(albumWebMapper.toSummaryDto(album));
    }

    @Operation(summary = "Get album details with paginated assets")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Album details"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlbumResponseDto> getAlbum(@PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int page) {
        try {
            UUID userId = resolveUserId();
            AlbumData summary = getAlbumSummaryUseCase.execute(id, userId);
            PaginatedResult<Asset> assetsPage = getAlbumAssetsUseCase.execute(id, userId, page);
            int totalPages = assetsPage.pageSize() > 0
                    ? (int) Math.ceil((double) assetsPage.total() / assetsPage.pageSize()) : 0;
            PaginatedData<AssetResponseDto> assetDtos = new PaginatedData<>(
                    assetsPage.items().stream().map(assetWebMapper::toDto).collect(Collectors.toList()),
                    page, totalPages, assetsPage.total());
            return ResponseEntity.ok(albumWebMapper.toDto(summary, assetDtos));
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update album name or description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Album updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AlbumSummaryResponseDto> updateAlbum(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAlbumRequestDto request) {
        try {
            AlbumData updated = updateAlbumUseCase.execute(id, resolveUserId(), request.name(), request.description(), serializeFilterJson(request.filterJson()));
            return ResponseEntity.ok(albumWebMapper.toSummaryDto(updated));
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete an album")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Album deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        try {
            deleteAlbumUseCase.execute(id, resolveUserId());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add assets to an album")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assets added"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Album not found"),
        @ApiResponse(responseCode = "422", description = "Smart album — manual membership forbidden")
    })
    @PostMapping("/{id}/assets")
    public ResponseEntity<Void> addAssets(@PathVariable Long id,
                                          @Valid @RequestBody AlbumAssetIdsRequestDto request) {
        try {
            addAssetsToAlbumUseCase.execute(id, resolveUserId(), request.assetIds());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Remove assets from an album")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assets removed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Album not found"),
        @ApiResponse(responseCode = "422", description = "Smart album — manual membership forbidden")
    })
    @DeleteMapping("/{id}/assets")
    public ResponseEntity<Void> removeAssets(@PathVariable Long id,
                                             @Valid @RequestBody AlbumAssetIdsRequestDto request) {
        try {
            removeAssetsFromAlbumUseCase.execute(id, resolveUserId(), request.assetIds());
            return ResponseEntity.noContent().build();
        } catch (AlbumNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UUID resolveUserId() {
        return getCurrentUserUseCase.execute().getId();
    }

    private String serializeFilterJson(AlbumFilterJson filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.writeValueAsString(filterJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize filterJson", e);
        }
    }
}
