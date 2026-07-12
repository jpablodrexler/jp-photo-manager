package com.jpablodrexler.photomanager.application.usecase.audit;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.in.audit.GetAuditLogUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAuditLogUseCaseImpl implements GetAuditLogUseCase {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<AuditEvent> execute(AuditLogFilter filter) {
        return auditLogRepository.findByFilters(filter);
    }
}
