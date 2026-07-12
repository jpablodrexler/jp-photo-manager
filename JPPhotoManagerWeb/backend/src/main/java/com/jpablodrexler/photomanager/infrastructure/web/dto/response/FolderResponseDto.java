package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class FolderResponseDto {

    private Long folderId;
    private String path;
    private String name;
    private String parentPath;
    private List<FolderResponseDto> children;
}
