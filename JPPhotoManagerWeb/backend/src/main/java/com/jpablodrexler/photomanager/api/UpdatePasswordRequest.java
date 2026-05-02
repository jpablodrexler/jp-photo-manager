package com.jpablodrexler.photomanager.api;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(@NotBlank String password) {}
