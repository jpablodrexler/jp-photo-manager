package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAlbumRequest(
        @NotBlank String name,
        String description
) {}
