package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAlbumRequest(
        @NotBlank String name,
        String description
) {}
