package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SearchPresetEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SearchPresetEntityMapper {

    @Mapping(source = "user.id", target = "userId")
    SearchPreset toDomain(SearchPresetEntity entity);
}
