package com.jpablodrexler.photomanager.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSummary(UUID id, String username, Instant createdAt) {}
