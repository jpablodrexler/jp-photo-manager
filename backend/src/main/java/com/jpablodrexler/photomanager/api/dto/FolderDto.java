package com.jpablodrexler.photomanager.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class FolderDto {

    private Long folderId;
    private String path;
    private String name;
    private String parentPath;
    private List<FolderDto> children;
}
