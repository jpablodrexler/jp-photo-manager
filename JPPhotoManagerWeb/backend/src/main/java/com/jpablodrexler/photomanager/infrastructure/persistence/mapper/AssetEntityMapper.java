package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FolderEntityMapper.class})
public interface AssetEntityMapper {

    Asset toDomain(AssetEntity entity);

    AssetEntity toEntity(Asset domain);
}
