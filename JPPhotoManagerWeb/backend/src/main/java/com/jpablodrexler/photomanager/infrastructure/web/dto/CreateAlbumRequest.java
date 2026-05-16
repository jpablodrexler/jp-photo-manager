package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAlbumRequest(
        @NotBlank String name,
        String description
) {}
