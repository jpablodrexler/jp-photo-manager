package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.HomeStatsResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.HomeWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Home", description = "Dashboard statistics")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final GetHomeStatsUseCase getHomeStatsUseCase;
    private final HomeWebMapper homeWebMapper;

    @Operation(summary = "Get dashboard statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Home statistics"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/stats")
    public HomeStatsResponseDto getStats() {
        return homeWebMapper.toDto(getHomeStatsUseCase.execute());
    }
}
