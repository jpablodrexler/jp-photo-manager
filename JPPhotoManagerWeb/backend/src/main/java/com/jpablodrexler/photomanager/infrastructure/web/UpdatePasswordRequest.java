package com.jpablodrexler.photomanager.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(@NotBlank String password) {}
