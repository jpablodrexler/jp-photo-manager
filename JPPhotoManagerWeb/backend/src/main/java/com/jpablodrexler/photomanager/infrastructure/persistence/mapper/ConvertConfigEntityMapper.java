package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.ConvertAssetsDirectoriesDefinitionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConvertConfigEntityMapper {

    ConvertDirectoriesDefinition toDomain(ConvertAssetsDirectoriesDefinitionEntity entity);

    ConvertAssetsDirectoriesDefinitionEntity toEntity(ConvertDirectoriesDefinition domain);
}
