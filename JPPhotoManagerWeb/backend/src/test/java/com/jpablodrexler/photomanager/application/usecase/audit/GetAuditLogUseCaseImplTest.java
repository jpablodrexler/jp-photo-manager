package com.jpablodrexler.photomanager.application.usecase.audit;

import com.jpablodrexler.photomanager.application.dto.AuditLogFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuditLogUseCaseImplTest {

    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks GetAuditLogUseCaseImpl sut;

    @Test
    void execute_delegatesToRepositoryWithSameFilter() {
        AuditLogFilter filter = new AuditLogFilter(UUID.randomUUID(), "42", null, null, 0, 20);
        PaginatedResult<AuditEvent> expected = new PaginatedResult<>(List.of(), 0L, 0, 20);
        when(auditLogRepository.findByFilters(filter)).thenReturn(expected);

        PaginatedResult<AuditEvent> result = sut.execute(filter);

        assertThat(result).isEqualTo(expected);
        verify(auditLogRepository).findByFilters(filter);
    }
}
