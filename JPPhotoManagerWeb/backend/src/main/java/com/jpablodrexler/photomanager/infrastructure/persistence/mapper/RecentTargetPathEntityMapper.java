package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RecentTargetPathEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecentTargetPathEntityMapper {

    RecentTargetPath toDomain(RecentTargetPathEntity entity);

    RecentTargetPathEntity toEntity(RecentTargetPath domain);
}
