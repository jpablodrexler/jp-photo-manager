package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SyncAssetsDirectoriesDefinitionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SyncConfigEntityMapper {

    SyncDirectoriesDefinition toDomain(SyncAssetsDirectoriesDefinitionEntity entity);

    SyncAssetsDirectoriesDefinitionEntity toEntity(SyncDirectoriesDefinition domain);
}
