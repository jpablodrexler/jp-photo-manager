package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.audit.GetAuditLogUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AuditLogEntryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AuditLogWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Audit Log", description = "Read-only access to the user-action audit trail")
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final GetAuditLogUseCase getAuditLogUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AuditLogWebMapper auditLogWebMapper;

    @Operation(summary = "List audit log entries, filterable by user, entity and time range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated audit log entries"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Non-admin user requested another user's audit history")
    })
    @GetMapping
    public ResponseEntity<PaginatedData<AuditLogEntryResponseDto>> getAuditLog(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        User currentUser = getCurrentUserUseCase.execute();
        AuditLogFilter filter = new AuditLogFilter(userId, entityId, from, to, page, size);
        PaginatedResult<AuditEvent> result = getAuditLogUseCase.execute(filter, currentUser);
        List<AuditLogEntryResponseDto> dtos = result.items().stream()
                .map(auditLogWebMapper::toDto)
                .collect(Collectors.toList());
        int totalPages = result.pageSize() > 0 ? (int) Math.ceil((double) result.total() / result.pageSize()) : 0;
        PaginatedData<AuditLogEntryResponseDto> data = new PaginatedData<>(dtos, page, totalPages, result.total());
        return ResponseEntity.ok(data);
    }
}
