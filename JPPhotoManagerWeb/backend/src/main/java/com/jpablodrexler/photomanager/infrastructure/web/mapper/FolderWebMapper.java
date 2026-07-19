package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.FolderResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FolderWebMapper {

    FolderResponseDto toDto(Folder folder);
}
