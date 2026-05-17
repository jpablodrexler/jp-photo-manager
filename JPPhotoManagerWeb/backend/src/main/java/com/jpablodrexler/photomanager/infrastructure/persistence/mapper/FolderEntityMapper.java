package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FolderEntityMapper {

    Folder toDomain(FolderEntity entity);

    FolderEntity toEntity(Folder domain);
}
