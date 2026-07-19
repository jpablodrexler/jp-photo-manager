package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequestDto(@NotBlank String username, @NotBlank String password) {}
