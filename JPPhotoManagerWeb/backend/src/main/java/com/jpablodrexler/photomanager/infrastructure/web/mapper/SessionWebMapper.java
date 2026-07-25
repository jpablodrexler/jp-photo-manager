package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.SessionInfo;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SessionResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessionWebMapper {

    SessionResponseDto toDto(SessionInfo sessionInfo);

    List<SessionResponseDto> toDtoList(List<SessionInfo> sessionInfos);
}
