package com.jpablodrexler.photomanager.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserSummary(UUID id, String username, Instant createdAt) {}
