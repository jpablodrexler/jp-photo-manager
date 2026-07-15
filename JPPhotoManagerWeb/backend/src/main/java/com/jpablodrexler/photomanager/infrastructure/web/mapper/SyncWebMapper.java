package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.SyncDirectoryPairDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SyncWebMapper {

    SyncDirectoryPairDto toDto(SyncDirectoriesDefinition definition);

    SyncDirectoriesDefinition toDomain(SyncDirectoryPairDto dto);

    List<SyncDirectoryPairDto> toDtoList(List<SyncDirectoriesDefinition> definitions);

    List<SyncDirectoriesDefinition> toDomainList(List<SyncDirectoryPairDto> dtos);
}
