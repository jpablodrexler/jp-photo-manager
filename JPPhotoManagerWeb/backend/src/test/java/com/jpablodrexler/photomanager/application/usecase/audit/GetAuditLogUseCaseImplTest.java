package com.jpablodrexler.photomanager.application.usecase.audit;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuditLogUseCaseImplTest {

    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks GetAuditLogUseCaseImpl sut;

    @Test
    void execute_adminRequestsAnotherUsersLog_passesRequestedUserIdThrough() {
        UUID targetUserId = UUID.randomUUID();
        User admin = User.builder().id(UUID.randomUUID()).role("ADMIN").build();
        AuditLogFilter filter = new AuditLogFilter(targetUserId, "42", null, null, 0, 20);
        PaginatedResult<AuditEvent> expected = new PaginatedResult<>(List.of(), 0L, 0, 20);
        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        when(auditLogRepository.findByFilters(filterCaptor.capture())).thenReturn(expected);

        PaginatedResult<AuditEvent> result = sut.execute(filter, admin);

        assertThat(result).isEqualTo(expected);
        assertThat(filterCaptor.getValue().userId()).isEqualTo(targetUserId);
    }

    @Test
    void execute_nonAdminRequestsOwnUserId_allowed() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role("USER").build();
        AuditLogFilter filter = new AuditLogFilter(userId, null, null, null, 0, 20);
        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        when(auditLogRepository.findByFilters(filterCaptor.capture()))
                .thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 20));

        sut.execute(filter, user);

        assertThat(filterCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void execute_nonAdminOmitsUserId_defaultsToOwnUserId() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role("USER").build();
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, 0, 20);
        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        when(auditLogRepository.findByFilters(filterCaptor.capture()))
                .thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 20));

        sut.execute(filter, user);

        assertThat(filterCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void execute_nonAdminRequestsAnotherUsersLog_throwsAccessDenied() {
        User user = User.builder().id(UUID.randomUUID()).role("USER").build();
        AuditLogFilter filter = new AuditLogFilter(UUID.randomUUID(), null, null, null, 0, 20);

        assertThatThrownBy(() -> sut.execute(filter, user))
                .isInstanceOf(AccessDeniedException.class);
    }
}
