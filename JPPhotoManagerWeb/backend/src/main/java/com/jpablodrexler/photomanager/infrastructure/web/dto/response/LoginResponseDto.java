package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;

public record LoginResponseDto(String username, Instant expiresAt) {}
