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

    private final GetDeletedAssetsUseCase getDeletedAssetsUseCase;
    private final RestoreAssetsUseCase restoreAssetsUseCase;
    private final PurgeAssetsUseCase purgeAssetsUseCase;
    private final AssetWebMapper assetWebMapper;

    @GetMapping
    public ResponseEntity<PaginatedData<AssetDto>> listDeleted(@RequestParam(defaultValue = "0") int page) {
        PaginatedResult<Asset> result = getDeletedAssetsUseCase.execute(page);
        int totalPages = result.pageSize() > 0 ? (int) Math.ceil((double) result.total() / result.pageSize()) : 0;
        PaginatedData<AssetDto> data = new PaginatedData<>(
                result.items().stream().map(assetWebMapper::toDto).collect(Collectors.toList()),
                page, totalPages, result.total());
        return ResponseEntity.ok(data);
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restore(@Valid @RequestBody RecycleBinRestoreRequest body) {
        restoreAssetsUseCase.execute(body.assetIds());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> purge(@RequestBody(required = false) RecycleBinPurgeRequest body) {
        List<Long> ids = (body != null) ? body.assetIds() : null;
        purgeAssetsUseCase.execute(ids);
        return ResponseEntity.noContent().build();
    }
}
