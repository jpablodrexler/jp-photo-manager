package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AlbumEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlbumEntityMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "assets", expression = "java(java.util.Collections.emptyList())")
    Album toDomain(AlbumEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "assets", ignore = true)
    AlbumEntity toEntity(Album domain);
}
