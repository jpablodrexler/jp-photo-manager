package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.port.in.search.CreateSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.DeleteSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.GetSearchPresetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreatePresetRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.SearchPresetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.SearchPresetWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Search Presets", description = "Saved search filter presets")
@RestController
@RequestMapping("/api/search-presets")
@RequiredArgsConstructor
public class SearchPresetController {

    private final GetSearchPresetsUseCase getSearchPresetsUseCase;
    private final CreateSearchPresetUseCase createSearchPresetUseCase;
    private final DeleteSearchPresetUseCase deleteSearchPresetUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final SearchPresetWebMapper searchPresetWebMapper;

    @Operation(summary = "List saved search presets for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search preset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public List<SearchPresetResponseDto> list() {
        List<SearchPreset> presets = getSearchPresetsUseCase.execute(resolveUserId());
        return presets.stream().map(searchPresetWebMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Create a new search preset")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Preset created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchPresetResponseDto create(@Valid @RequestBody CreatePresetRequestDto body) {
        FilterPreset criteria = new FilterPreset(body.search(), body.dateFrom(), body.dateTo(), body.minRating());
        SearchPreset preset = createSearchPresetUseCase.execute(resolveUserId(), body.name(), criteria);
        return searchPresetWebMapper.toDto(preset);
    }

    @Operation(summary = "Delete a search preset")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Preset deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Preset not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteSearchPresetUseCase.execute(id, resolveUserId());
    }

    private UUID resolveUserId() {
        return getCurrentUserUseCase.execute().getId();
    }
}
