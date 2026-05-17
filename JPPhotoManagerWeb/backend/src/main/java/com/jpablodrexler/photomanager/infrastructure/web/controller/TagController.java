package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final ListTagsUseCase listTagsUseCase;

    @GetMapping
    public ResponseEntity<List<String>> searchTags(@RequestParam(required = false) String q) {
        List<String> names = listTagsUseCase.execute(q).stream()
                .map(tag -> tag.getName())
                .toList();
        return ResponseEntity.ok(names);
    }
}
