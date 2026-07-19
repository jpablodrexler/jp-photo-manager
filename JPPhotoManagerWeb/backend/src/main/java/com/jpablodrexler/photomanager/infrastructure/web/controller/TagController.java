package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
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

import java.util.List;

@Tag(name = "Tags", description = "Asset tag search and discovery")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final ListTagsUseCase listTagsUseCase;

    @Operation(summary = "Search tags by prefix")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching tag names"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<String>> searchTags(@RequestParam(required = false) String q) {
        List<String> names = listTagsUseCase.execute(q).stream()
                .map(tag -> tag.getName())
                .toList();
        return ResponseEntity.ok(names);
    }
}
