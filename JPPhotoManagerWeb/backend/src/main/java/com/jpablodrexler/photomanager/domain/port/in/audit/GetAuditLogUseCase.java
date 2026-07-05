package com.jpablodrexler.photomanager.domain.port.in.audit;

import com.jpablodrexler.photomanager.application.dto.AuditLogFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;

public interface GetAuditLogUseCase {
    PaginatedResult<AuditEvent> execute(AuditLogFilter filter);
}
