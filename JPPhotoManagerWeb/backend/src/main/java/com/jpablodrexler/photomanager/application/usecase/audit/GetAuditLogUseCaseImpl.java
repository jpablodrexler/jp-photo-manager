package com.jpablodrexler.photomanager.application.usecase.audit;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.audit.GetAuditLogUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAuditLogUseCaseImpl implements GetAuditLogUseCase {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<AuditEvent> execute(AuditLogFilter filter, User requestingUser) {
        boolean isAdmin = ADMIN_ROLE.equals(requestingUser.getRole());
        UUID userId = filter.userId();

        if (!isAdmin) {
            if (userId != null && !userId.equals(requestingUser.getId())) {
                throw new AccessDeniedException("Cannot view another user's audit history");
            }
            userId = requestingUser.getId();
        }

        AuditLogFilter scopedFilter = new AuditLogFilter(userId, filter.entityId(), filter.from(), filter.to(),
                filter.page(), filter.pageSize());
        return auditLogRepository.findByFilters(scopedFilter);
    }
}
