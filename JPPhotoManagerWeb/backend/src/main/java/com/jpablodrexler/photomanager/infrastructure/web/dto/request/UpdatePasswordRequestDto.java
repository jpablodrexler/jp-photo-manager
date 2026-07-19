package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequestDto(@NotBlank String password) {}
