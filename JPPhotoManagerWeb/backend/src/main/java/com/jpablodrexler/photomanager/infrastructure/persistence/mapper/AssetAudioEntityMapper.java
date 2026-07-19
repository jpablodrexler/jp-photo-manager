package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetAudioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AssetAudioEntityMapper {

    @Mapping(source = "asset.assetId", target = "assetId")
    AssetAudio toDomain(AssetAudioEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromDomain(AssetAudio domain, @MappingTarget AssetAudioEntity entity);
}
