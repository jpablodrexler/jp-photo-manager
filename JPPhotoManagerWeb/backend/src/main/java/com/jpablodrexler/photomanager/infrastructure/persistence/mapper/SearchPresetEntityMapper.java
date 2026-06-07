package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SearchPresetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SearchPresetEntityMapper {

    @Mapping(source = "user.id", target = "userId")
    SearchPreset toDomain(SearchPresetEntity entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUserEntityRef")
    SearchPresetEntity toEntity(SearchPreset domain);

    @Named("userIdToUserEntityRef")
    default UserEntity userIdToUserEntityRef(UUID userId) {
        if (userId == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(userId);
        return ref;
    }
}
