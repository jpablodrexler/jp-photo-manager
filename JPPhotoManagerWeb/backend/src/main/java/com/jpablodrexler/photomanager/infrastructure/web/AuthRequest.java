package com.jpablodrexler.photomanager.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(@NotBlank String username, @NotBlank String password) {
}
