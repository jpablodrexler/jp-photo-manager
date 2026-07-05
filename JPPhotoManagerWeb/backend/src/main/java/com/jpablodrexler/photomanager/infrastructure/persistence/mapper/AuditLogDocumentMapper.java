package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogDocumentMapper {

    @Mapping(target = "id", ignore = true)
    AuditLogDocument toDocument(AuditEvent event);

    AuditEvent toDomain(AuditLogDocument document);
}
