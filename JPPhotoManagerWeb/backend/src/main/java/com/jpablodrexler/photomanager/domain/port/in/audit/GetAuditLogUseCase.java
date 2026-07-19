package com.jpablodrexler.photomanager.domain.port.in.audit;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.User;

public interface GetAuditLogUseCase {
    PaginatedResult<AuditEvent> execute(AuditLogFilter filter, User requestingUser);
}
