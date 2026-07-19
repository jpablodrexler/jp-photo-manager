package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagEntityMapper {

    Tag toDomain(TagEntity entity);

    TagEntity toEntity(Tag domain);
}
