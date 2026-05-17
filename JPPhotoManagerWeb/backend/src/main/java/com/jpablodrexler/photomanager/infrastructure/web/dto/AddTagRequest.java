package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTagRequest(@NotBlank String name) {}
