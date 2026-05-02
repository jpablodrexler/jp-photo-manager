package com.jpablodrexler.photomanager.api;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String username, @NotBlank String password) {}
