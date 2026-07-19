package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.analytics.GetAnalyticsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AnalyticsResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AnalyticsWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Storage and usage analytics")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final GetAnalyticsUseCase getAnalyticsUseCase;
    private final AnalyticsWebMapper analyticsWebMapper;

    @Operation(summary = "Get analytics data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<AnalyticsResponseDto> getAnalytics() {
        return ResponseEntity.ok(analyticsWebMapper.toDto(getAnalyticsUseCase.execute()));
    }
}
