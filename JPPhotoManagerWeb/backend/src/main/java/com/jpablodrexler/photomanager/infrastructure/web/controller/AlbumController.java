package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.album.AddAssetsToAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.CreateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.DeleteAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.RemoveAssetsFromAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.in.album.UpdateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AlbumAssetIdsRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AlbumDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AlbumSummaryDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.CreateAlbumRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.UpdateAlbumRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final GetAlbumUseCase getAlbumUseCase;
    private final UpdateAlbumUseCase updateAlbumUseCase;
    private final DeleteAlbumUseCase deleteAlbumUseCase;
    private final AddAssetsToAlbumUseCase addAssetsToAlbumUseCase;
    private final RemoveAssetsFromAlbumUseCase removeAssetsFromAlbumUseCase;
    private final UserRepository userRepository;
    private final AlbumWebMapper albumWebMapper;
    private final AssetWebMapper assetWebMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "List albums for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Album list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<AlbumSummaryDto>> listAlbums() {
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
    public ResponseEntity<AlbumSummaryDto> createAlbum(@Valid @RequestBody CreateAlbumRequest request) {
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
    public ResponseEntity<AlbumDto> getAlbum(@PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int page) {
        try {
            UUID userId = resolveUserId();
            AlbumData summary = getAlbumUseCase.executeSummary(id, userId);
            PaginatedResult<Asset> assetsPage = getAlbumUseCase.executeAssets(id, userId, page);
            int totalPages = assetsPage.pageSize() > 0
                    ? (int) Math.ceil((double) assetsPage.total() / assetsPage.pageSize()) : 0;
            PaginatedData<AssetDto> assetDtos = new PaginatedData<>(
                    assetsPage.items().stream().map(assetWebMapper::toDto).collect(Collectors.toList()),
                    page, totalPages, assetsPage.total());
            AlbumDto dto = new AlbumDto();
            dto.setAlbumId(summary.albumId());
            dto.setName(summary.name());
            dto.setDescription(summary.description());
            dto.setCreatedAt(summary.createdAt());
            dto.setAssets(assetDtos);
            dto.setFilterJson(deserializeFilterJson(summary.filterJson()));
            return ResponseEntity.ok(dto);
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
    public ResponseEntity<AlbumSummaryDto> updateAlbum(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAlbumRequest request) {
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
                                          @Valid @RequestBody AlbumAssetIdsRequest request) {
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
                                             @Valid @RequestBody AlbumAssetIdsRequest request) {
        try {
            removeAssetsFromAlbumUseCase.execute(id, resolveUserId(), request.assetIds());
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

    private String serializeFilterJson(AlbumFilterJson filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.writeValueAsString(filterJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize filterJson", e);
        }
    }

    private AlbumFilterJson deserializeFilterJson(String filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.readValue(filterJson, AlbumFilterJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize filterJson", e);
        }
    }
}
