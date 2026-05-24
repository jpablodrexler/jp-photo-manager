package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetDrivesUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetInitialFolderUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetRecentTargetPathsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetSubFoldersUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.FolderDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.FolderWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Folders", description = "Folder navigation and discovery")
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final GetSubFoldersUseCase getSubFoldersUseCase;
    private final GetDrivesUseCase getDrivesUseCase;
    private final GetInitialFolderUseCase getInitialFolderUseCase;
    private final GetRecentTargetPathsUseCase getRecentTargetPathsUseCase;
    private final FolderWebMapper folderWebMapper;

    @Operation(summary = "List catalogued folders, optionally filtered by parent path")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Folder list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<FolderDto>> getFolders(@RequestParam(required = false) String parentPath) {
        List<Folder> folders = getSubFoldersUseCase.execute(parentPath);
        return ResponseEntity.ok(folders.stream().map(folderWebMapper::toDto).collect(Collectors.toList()));
    }

    @Operation(summary = "List available filesystem roots / drives")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Drive list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/drives")
    public ResponseEntity<List<String>> getDrives() {
        return ResponseEntity.ok(getDrivesUseCase.execute());
    }

    @Operation(summary = "Get the configured initial folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Initial folder path"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/initial")
    public ResponseEntity<String> getInitialFolder() {
        return ResponseEntity.ok(getInitialFolderUseCase.execute());
    }

    @Operation(summary = "Get recently used destination paths")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recent paths list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/recent-paths")
    public ResponseEntity<List<String>> getRecentTargetPaths() {
        return ResponseEntity.ok(getRecentTargetPathsUseCase.execute());
    }
}
