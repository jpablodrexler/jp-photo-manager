package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.web.dto.FolderDto;
import org.springframework.stereotype.Component;

@Component
public class FolderWebMapper {

    public FolderDto toDto(Folder folder) {
        FolderDto dto = new FolderDto();
        dto.setFolderId(folder.getFolderId());
        dto.setPath(folder.getPath());
        dto.setName(folder.getName());
        dto.setParentPath(folder.getParentPath());
        return dto;
    }
}
