package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserPreferenceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPreferenceMapper {
    UserPreference toDomain(UserPreferenceEntity entity);
    UserPreferenceEntity toEntity(UserPreference domain);
}
