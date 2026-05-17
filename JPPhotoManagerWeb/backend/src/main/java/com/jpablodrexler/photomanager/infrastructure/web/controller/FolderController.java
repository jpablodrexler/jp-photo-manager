package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetDrivesUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetInitialFolderUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetRecentTargetPathsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetSubFoldersUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.FolderDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.FolderWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final GetSubFoldersUseCase getSubFoldersUseCase;
    private final GetDrivesUseCase getDrivesUseCase;
    private final GetInitialFolderUseCase getInitialFolderUseCase;
    private final GetRecentTargetPathsUseCase getRecentTargetPathsUseCase;
    private final FolderWebMapper folderWebMapper;

    @GetMapping
    public ResponseEntity<List<FolderDto>> getFolders(@RequestParam(required = false) String parentPath) {
        List<Folder> folders = getSubFoldersUseCase.execute(parentPath);
        return ResponseEntity.ok(folders.stream().map(folderWebMapper::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/drives")
    public ResponseEntity<List<String>> getDrives() {
        return ResponseEntity.ok(getDrivesUseCase.execute());
    }

    @GetMapping("/initial")
    public ResponseEntity<String> getInitialFolder() {
        return ResponseEntity.ok(getInitialFolderUseCase.execute());
    }

    @GetMapping("/recent-paths")
    public ResponseEntity<List<String>> getRecentTargetPaths() {
        return ResponseEntity.ok(getRecentTargetPathsUseCase.execute());
    }
}
