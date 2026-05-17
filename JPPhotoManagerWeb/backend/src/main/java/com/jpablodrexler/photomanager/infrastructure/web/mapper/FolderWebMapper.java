package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.web.dto.FolderDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FolderWebMapper {

    FolderDto toDto(Folder folder);
}
