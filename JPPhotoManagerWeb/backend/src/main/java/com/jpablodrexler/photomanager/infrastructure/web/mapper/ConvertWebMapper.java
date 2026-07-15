package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.ConvertDirectoryPairDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConvertWebMapper {

    ConvertDirectoryPairDto toDto(ConvertDirectoriesDefinition definition);

    ConvertDirectoriesDefinition toDomain(ConvertDirectoryPairDto dto);

    List<ConvertDirectoryPairDto> toDtoList(List<ConvertDirectoriesDefinition> definitions);

    List<ConvertDirectoriesDefinition> toDomainList(List<ConvertDirectoryPairDto> dtos);
}
