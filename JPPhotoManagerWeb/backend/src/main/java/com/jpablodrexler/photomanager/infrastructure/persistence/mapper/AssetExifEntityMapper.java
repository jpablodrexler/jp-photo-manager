package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssetExifEntityMapper {

    AssetExif toDomain(AssetExifEntity entity);
}
