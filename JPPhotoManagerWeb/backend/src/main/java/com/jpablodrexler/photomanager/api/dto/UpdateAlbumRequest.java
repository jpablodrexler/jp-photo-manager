package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAlbumRequest(
        @NotBlank String name,
        String description
) {}
