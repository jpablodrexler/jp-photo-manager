package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.recycle.GetDeletedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.recycle.PurgeAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.recycle.RestoreAssetsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RecycleBinPurgeRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.RecycleBinRestoreRequest;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Tag(name = "Recycle Bin", description = "Soft-deleted asset management")
@RestController
@RequestMapping("/api/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {

    private final GetDeletedAssetsUseCase getDeletedAssetsUseCase;
    private final RestoreAssetsUseCase restoreAssetsUseCase;
    private final PurgeAssetsUseCase purgeAssetsUseCase;
    private final AssetWebMapper assetWebMapper;

    @Operation(summary = "List soft-deleted assets")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated deleted asset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> listDeleted(@RequestParam(defaultValue = "0") int page) {
        PaginatedResult<Asset> result = getDeletedAssetsUseCase.execute(page);
        int totalPages = result.pageSize() > 0 ? (int) Math.ceil((double) result.total() / result.pageSize()) : 0;
        PaginatedData<AssetDto> data = new PaginatedData<>(
                result.items().stream().map(assetWebMapper::toDto).collect(Collectors.toList()),
                page, totalPages, result.total());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Restore assets from the recycle bin")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assets restored"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/restore")
    public ResponseEntity<Void> restore(@Valid @RequestBody RecycleBinRestoreRequest body) {
        restoreAssetsUseCase.execute(body.assetIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently purge assets from the recycle bin")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assets purged"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping
    public ResponseEntity<Void> purge(@Valid @RequestBody RecycleBinPurgeRequest body) {
        purgeAssetsUseCase.execute(body.assetIds());
        return ResponseEntity.noContent().build();
    }
}
