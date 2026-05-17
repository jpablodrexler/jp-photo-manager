package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserEntityMapper.class})
public interface RefreshTokenEntityMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    @Mapping(target = "user", source = "user", qualifiedByName = "toUserEntityRef")
    RefreshTokenEntity toEntity(RefreshToken domain);
}
