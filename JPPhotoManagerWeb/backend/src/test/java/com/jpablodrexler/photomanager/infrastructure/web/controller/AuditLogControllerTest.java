package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.audit.GetAuditLogUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AuditLogWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@ActiveProfiles("test")
class AuditLogControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean GetAuditLogUseCase getAuditLogUseCase;
    @MockitoBean GetCurrentUserUseCase getCurrentUserUseCase;
    @MockitoBean AuditLogWebMapper auditLogWebMapper;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(username = "user", roles = "VIEWER")
    void getAuditLog_nonAdminNoUserIdParam_defaultsToOwnUserIdAndReturns200() throws Exception {
        User user = User.builder().id(userId).username("user").role("VIEWER").build();
        when(getCurrentUserUseCase.execute()).thenReturn(user);
        when(getAuditLogUseCase.execute(any(), any())).thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 50));

        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

        verify(getAuditLogUseCase).execute(any(), argThat(u -> userId.equals(u.getId())));
    }

    @Test
    @WithMockUser(username = "user", roles = "VIEWER")
    void getAuditLog_nonAdminRequestsOwnUserIdExplicitly_returns200() throws Exception {
        User user = User.builder().id(userId).username("user").role("VIEWER").build();
        when(getCurrentUserUseCase.execute()).thenReturn(user);
        when(getAuditLogUseCase.execute(any(), any())).thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 50));

        mockMvc.perform(get("/api/audit-log").param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "VIEWER")
    void getAuditLog_nonAdminRequestsAnotherUserId_returns403() throws Exception {
        User user = User.builder().id(userId).username("user").role("VIEWER").build();
        when(getCurrentUserUseCase.execute()).thenReturn(user);
        when(getAuditLogUseCase.execute(any(), any()))
                .thenThrow(new AccessDeniedException("Cannot view another user's audit history"));

        mockMvc.perform(get("/api/audit-log").param("userId", otherUserId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLog_adminRequestsAnotherUserId_returns200() throws Exception {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();
        when(getCurrentUserUseCase.execute()).thenReturn(admin);
        when(getAuditLogUseCase.execute(any(), any())).thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 50));

        mockMvc.perform(get("/api/audit-log").param("userId", otherUserId.toString()))
                .andExpect(status().isOk());

        verify(getAuditLogUseCase).execute(argThat(filter -> otherUserId.equals(filter.userId())), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLog_adminOmitsUserId_queriesAcrossAllUsers() throws Exception {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();
        when(getCurrentUserUseCase.execute()).thenReturn(admin);
        when(getAuditLogUseCase.execute(any(), any())).thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 50));

        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isOk());

        verify(getAuditLogUseCase).execute(argThat(filter -> filter.userId() == null), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLog_entityIdAndTimeRangeFilters_passedThroughToUseCase() throws Exception {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();
        when(getCurrentUserUseCase.execute()).thenReturn(admin);
        when(getAuditLogUseCase.execute(any(), any())).thenReturn(new PaginatedResult<>(List.of(), 0L, 0, 50));

        mockMvc.perform(get("/api/audit-log")
                        .param("entityId", "15")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk());

        verify(getAuditLogUseCase).execute(argThat(filter ->
                "15".equals(filter.entityId()) && filter.from() != null && filter.to() != null), any());
    }
}
