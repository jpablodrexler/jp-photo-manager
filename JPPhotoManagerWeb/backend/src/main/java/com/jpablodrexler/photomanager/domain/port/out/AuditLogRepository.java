package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;

public interface AuditLogRepository {

    void log(AuditEvent event);

    PaginatedResult<AuditEvent> findByFilters(AuditLogFilter filter);
}
