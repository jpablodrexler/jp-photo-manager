package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AuditLogEntryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogWebMapper {

    AuditLogEntryResponse toDto(AuditEvent event);
}
