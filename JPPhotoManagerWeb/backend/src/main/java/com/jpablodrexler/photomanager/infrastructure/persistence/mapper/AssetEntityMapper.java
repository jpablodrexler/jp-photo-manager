package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {FolderEntityMapper.class})
public interface AssetEntityMapper {

    @Mapping(source = "tags", target = "tags", qualifiedByName = "tagEntitiesToNames")
    Asset toDomain(AssetEntity entity);

    @Mapping(target = "tags", ignore = true)
    @Mapping(source = "folder", target = "folder", qualifiedByName = "toFolderEntityRef")
    AssetEntity toEntity(Asset domain);

    @Named("tagEntitiesToNames")
    default Set<String> tagEntitiesToNames(Set<TagEntity> tags) {
        if (tags == null) return Collections.emptySet();
        return tags.stream().map(TagEntity::getName).collect(Collectors.toSet());
    }
}
