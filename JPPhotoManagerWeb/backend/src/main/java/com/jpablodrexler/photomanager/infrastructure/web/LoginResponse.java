package com.jpablodrexler.photomanager.infrastructure.web;

import java.time.Instant;

public record LoginResponse(String username, Instant expiresAt) {}
