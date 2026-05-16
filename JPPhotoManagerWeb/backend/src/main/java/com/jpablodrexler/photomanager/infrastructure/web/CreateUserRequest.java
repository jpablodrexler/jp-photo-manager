package com.jpablodrexler.photomanager.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String username, @NotBlank String password) {}
