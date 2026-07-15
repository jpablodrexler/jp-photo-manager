package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.UserSummaryResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserAdminWebMapper {

    UserSummaryResponseDto toDto(UserSummary summary);

    List<UserSummaryResponseDto> toDtoList(List<UserSummary> summaries);
}
