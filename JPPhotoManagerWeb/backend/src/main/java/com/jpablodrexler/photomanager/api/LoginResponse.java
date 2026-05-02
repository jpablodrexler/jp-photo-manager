package com.jpablodrexler.photomanager.api;

import java.time.Instant;

public record LoginResponse(String username, Instant expiresAt) {}
