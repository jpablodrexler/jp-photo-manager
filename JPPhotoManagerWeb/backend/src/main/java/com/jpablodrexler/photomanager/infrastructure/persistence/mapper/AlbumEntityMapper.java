package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AlbumEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AlbumEntityMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "assets", expression = "java(java.util.Collections.emptyList())")
    Album toDomain(AlbumEntity entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUserEntityRef")
    @Mapping(target = "assets", ignore = true)
    AlbumEntity toEntity(Album domain);

    @Named("userIdToUserEntityRef")
    default UserEntity userIdToUserEntityRef(UUID userId) {
        if (userId == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(userId);
        return ref;
    }
}
