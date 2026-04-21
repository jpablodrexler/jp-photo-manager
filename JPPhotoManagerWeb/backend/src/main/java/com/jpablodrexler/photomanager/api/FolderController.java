package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.dto.FolderDto;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FolderController {

    private final PhotoManagerFacade facade;

    @GetMapping
    public ResponseEntity<List<FolderDto>> getFolders(
            @RequestParam(required = false) String parentPath) {
        List<Folder> folders = facade.getSubFolders(parentPath);
        List<FolderDto> dtos = folders.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/drives")
    public ResponseEntity<List<String>> getDrives() {
        return ResponseEntity.ok(facade.getDrives());
    }

    @GetMapping("/initial")
    public ResponseEntity<String> getInitialFolder() {
        return ResponseEntity.ok(facade.getInitialFolder());
    }

    @GetMapping("/recent-paths")
    public ResponseEntity<List<String>> getRecentTargetPaths() {
        return ResponseEntity.ok(facade.getRecentTargetPaths());
    }

    private FolderDto toDto(Folder folder) {
        FolderDto dto = new FolderDto();
        dto.setFolderId(folder.getFolderId());
        dto.setPath(folder.getPath());
        dto.setName(folder.getName());
        dto.setParentPath(folder.getParentPath());
        return dto;
    }
}
