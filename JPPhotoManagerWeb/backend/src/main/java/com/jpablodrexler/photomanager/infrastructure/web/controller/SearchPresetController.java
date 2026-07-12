package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.search.CreateSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.DeleteSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.search.GetSearchPresetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.web.dto.CreatePresetRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.SearchPresetDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Search Presets", description = "Saved search filter presets")
@RestController
@RequestMapping("/api/search-presets")
@RequiredArgsConstructor
@Slf4j
public class SearchPresetController {

    private final GetSearchPresetsUseCase getSearchPresetsUseCase;
    private final CreateSearchPresetUseCase createSearchPresetUseCase;
    private final DeleteSearchPresetUseCase deleteSearchPresetUseCase;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Operation(summary = "List saved search presets for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search preset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public List<SearchPresetDto> list() {
        List<SearchPreset> presets = getSearchPresetsUseCase.execute(resolveUserId());
        return presets.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Create a new search preset")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Preset created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchPresetDto create(@Valid @RequestBody CreatePresetRequest body) {
        FilterPreset criteria = new FilterPreset(body.search(), body.dateFrom(), body.dateTo(), body.minRating());
        SearchPreset preset = createSearchPresetUseCase.execute(resolveUserId(), body.name(), criteria);
        return toDto(preset);
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));
    }

    private SearchPresetDto toDto(SearchPreset preset) {
        FilterPreset filter = parseFilterJson(preset.getFilterJson());
        return new SearchPresetDto(
                preset.getPresetId(),
                preset.getName(),
                preset.getCreatedAt(),
                filter != null ? filter.search() : null,
                filter != null ? filter.dateFrom() : null,
                filter != null ? filter.dateTo() : null,
                filter != null ? filter.minRating() : null
        );
    }

    private FilterPreset parseFilterJson(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(filterJson, FilterPreset.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse filterJson: {}", filterJson, e);
            return null;
        }
    }
}
